/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package bio.overture.score.server.repository.s3;

import static bio.overture.score.server.metadata.MetadataService.getAnalysisId;
import static com.google.common.base.Preconditions.checkArgument;

import bio.overture.score.core.model.ObjectKey;
import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.core.model.Part;
import bio.overture.score.core.util.MD5s;
import bio.overture.score.core.util.ObjectKeys;
import bio.overture.score.core.util.PartCalculator;
import bio.overture.score.server.config.S3Config;
import bio.overture.score.server.exception.IdNotFoundException;
import bio.overture.score.server.exception.InternalUnrecoverableError;
import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.exception.RetryableException;
import bio.overture.score.server.metadata.MetadataEntity;
import bio.overture.score.server.metadata.MetadataService;
import bio.overture.score.server.repository.BucketNamingService;
import bio.overture.score.server.repository.DownloadService;
import bio.overture.score.server.repository.URLGenerator;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.Cleanup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** service responsible for object download (full or partial) */
@Slf4j
@Setter
@Service
@Profile({"aws", "collaboratory", "default"})
public class S3DownloadService implements DownloadService {

  /** Constants. */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String PUBLISHED_ANALYSIS_STATE = "PUBLISHED";

  /** Configuration. */
  @Value("${collaboratory.data.directory}")
  private String dataDir;

  @Value("${collaboratory.download.expiration}")
  private int expiration;

  @Value("${object.sentinel}")
  private String sentinelObjectId;

  @Value("${metadata.useLegacyMode:false}")
  private boolean useLegacyMode;

  /** Dependencies. */
  @Autowired private AmazonS3 s3Client;

  @Autowired private BucketNamingService bucketNamingService;
  @Autowired private URLGenerator urlGenerator;
  @Autowired private PartCalculator partCalculator;
  @Autowired private MetadataService metadataService;
  @Autowired private S3Config s3config;

  @Override
  public ObjectSpecification download(
      String objectId, long offset, long length, boolean forExternalUse, boolean excludeUrls) {
    try {
      if (!excludeUrls) {
        checkPublishedAnalysisState(metadataService.getEntity(objectId));
      }
      checkArgument(offset > -1L);

      val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);

      var objectSpec = getSpecification(objectId);

      if (objectSpec == null) {
        ObjectMetadata metadata =
            s3Client.getObjectMetadata(
                bucketNamingService.getObjectBucketName(objectId), objectKey.getKey());

        List<Part> parts;
        if (forExternalUse) {
          // Return as a single part - no matter how large
          parts = partCalculator.specify(0L, -1L);
        } else if (length < 0L) {
          parts = partCalculator.divide(offset, metadata.getContentLength() - offset);
        } else {
          parts = partCalculator.divide(offset, length);
        }
        fillPartUrls(objectKey, parts, false, forExternalUse);

        val md5 = getObjectMd5(metadata);

        objectSpec =
            new ObjectSpecification(
                objectKey.getKey(),
                objectId,
                objectId,
                parts,
                metadata.getContentLength(),
                md5,
                false);
      }

      // Short-circuit in default case
      if (!forExternalUse && (offset == 0L && length < 0L)) {
        return excludeUrls ? removeUrls(objectSpec) : objectSpec;
      }

      // Construct ObjectSpecification for actual object in /data logical folder
      // Calculate range values
      // To retrieve to the end of the file
      if (!forExternalUse && (length < 0L)) {
        length = objectSpec.getObjectSize() - offset;
      }

      // Validate offset and length parameters:
      // Check if the offset + length > length - that would be too big
      if ((offset + length) > objectSpec.getObjectSize()) {
        throw new InternalUnrecoverableError(
            "Specified parameters exceed object size (object id: "
                + objectId
                + ", offset: "
                + offset
                + ", length: "
                + length
                + ")");
      }

      List<Part> parts;
      if (forExternalUse) {
        // Return as a single part - no matter how large
        parts = partCalculator.specify(0L, -1L);
      } else {
        parts = partCalculator.divide(offset, length);
      }

      fillPartUrls(objectKey, parts, objectSpec.isRelocated(), forExternalUse);

      val spec =
          new ObjectSpecification(
              objectKey.getKey(),
              objectId,
              objectId,
              parts,
              length,
              objectSpec.getObjectMd5(),
              objectSpec.isRelocated());

      return excludeUrls ? removeUrls(spec) : spec;
    } catch (Exception e) {
      log.error(
          "Failed to download objectId: {}, offset: {}, length: {}, forExternalUse: {}, excludeUrls: {} : {} ",
          objectId,
          offset,
          length,
          forExternalUse,
          excludeUrls,
          e);

      throw e;
    }
  }

  /**
   * Looks for MD5 hash in the object metadata. This is used as part of the fallback behaviour when
   * the .meta file cannot be found. To find the MD5, we will look for a value using the built in S3
   * getContendMD5(), and if no value is found there we will check in a configurable user meta data
   * property. The name of this property is configurable via the S3 Config. If no MD5 value can be
   * found in either of these locations then it will be returned null.
   *
   * <p>A user can still download files with the MD5 set to null, but they will always fail to
   * validate through the CLI. To complete a download of a file in this state, the user should add
   * the argument to their CLI download command: --validate false
   *
   * @param metadata
   * @return
   */
  private String getObjectMd5(ObjectMetadata metadata) {
    val contentMd5 = metadata.getContentMD5();
    if (contentMd5 != null) {
      return MD5s.toHex(contentMd5);
    }
    val userMetadataMd5 =
        metadata.getUserMetaDataOf(s3config.getCustomMd5Property()); // get literal from config
    if (userMetadataMd5 != null) {
      return MD5s.toHex(userMetadataMd5);
    }

    // No value found, returning null.
    return null;
  }

  private static ObjectSpecification removeUrls(ObjectSpecification spec) {
    spec.getParts().forEach(x -> x.setUrl(null));
    return spec;
  }

  void checkPublishedAnalysisState(MetadataEntity entity) {
    if (!useLegacyMode) {
      val objectId = entity.getId();
      val analysisState = metadataService.getAnalysisStateForMetadata(entity);
      if (!analysisState.equals(PUBLISHED_ANALYSIS_STATE)) {
        val message =
            String.format(
                "Critical Error: cannot complete download for objectId '%s' with "
                    + "analysisState '%s' and analysisId '%s'. "
                    + "Can only download objects that have the analysisState '%s' or when the 'exclude-urls=true' flag is set. Update the file metadata or url parameters and retry.",
                objectId, analysisState, getAnalysisId(entity), PUBLISHED_ANALYSIS_STATE);
        log.error(message); // Log to audit log file
        throw new NotRetryableException(new IllegalStateException(message));
      }
    }
  }

  public ObjectSpecification getSpecification(String objectId) {
    val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
    val objectMetaKey = ObjectKeys.getObjectMetaKey(dataDir, objectId);
    log.debug(
        "Getting specification for objectId: {}, objectKey: {}, objectMetaKey: {}",
        objectId,
        objectKey,
        objectMetaKey);

    try {
      // Retrieve .meta file to get list of pre-signed URL's
      // also returns flag indicating whether the object was not in the expected partitioned bucket
      val obj = getObject(objectId, objectMetaKey);

      val spec = readSpecification(obj.getS3Object());
      spec.setRelocated(obj.isRelocated());

      // We do this now in case we are returning it immediately in download() call
      fillPartUrls(objectKey, spec.getParts(), obj.isRelocated(), false);

      return spec;
    } catch (JsonParseException | JsonMappingException e) {
      log.error(
          "Error reading specification for objectId: {}, objectMetaKey: {}, objectKey: {}: {}",
          objectId,
          objectMetaKey,
          objectKey,
          e);
      throw new NotRetryableException(e);
    } catch (IOException e) {
      log.error(
          "Failed to get specification for objectId: {}, objectMetaKey: {}, objectKey: {}: {}",
          objectId,
          objectMetaKey,
          objectKey,
          e);
      throw new NotRetryableException(e);
    } catch (IdNotFoundException e) {
      return null;
    }
  }

  private ObjectSpecification readSpecification(S3Object obj)
      throws JsonParseException, JsonMappingException, IOException {
    @Cleanup val inputStream = obj.getObjectContent();
    return MAPPER.readValue(inputStream, ObjectSpecification.class);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.icgc.dcc.storage.server.service.download.DownloadService#getSentinelObject()
   */
  @Override
  public String getSentinelObject() {
    if ((sentinelObjectId == null) || (sentinelObjectId.isEmpty())) {
      throw new NotRetryableException(
          new IllegalArgumentException("Sentinel object id not defined"));
    }
    val now = LocalDateTime.now();
    val expirationDate = Date.from(now.plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant());

    return urlGenerator.getDownloadUrl(
        bucketNamingService.getObjectBucketName("", true),
        ObjectKeys.getObjectKey(dataDir, sentinelObjectId),
        expirationDate);
  }

  /*
   * Retrieve meta file object
   */
  private S3FetchedObject getObject(String objectId, String objectMetaKey) {
    String stateBucketName = bucketNamingService.getStateBucketName(objectId);
    try {
      return fetchObject(stateBucketName, objectMetaKey);
    } catch (AmazonServiceException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        if (bucketNamingService.isPartitioned()) {
          // Try again with master bucket
          log.warn(
              "Object with objectId: {} not found in {}, objectKey: {}: {}. Trying master bucket {}",
              objectId,
              stateBucketName,
              objectMetaKey,
              e,
              bucketNamingService.getBaseStateBucketName());
          try {
            stateBucketName = bucketNamingService.getBaseStateBucketName(); // use base bucket name
            val obj = fetchObject(stateBucketName, objectMetaKey);
            obj.setRelocated(true);
            return obj;
          } catch (AmazonServiceException e2) {
            log.error(
                "Failed to get object with objectId: {} from {}, objectKey: {}: {}",
                objectId,
                stateBucketName,
                objectMetaKey,
                e);
            if ((e.getStatusCode() == HttpStatus.NOT_FOUND.value()) || (!e.isRetryable())) {
              throw new IdNotFoundException(objectId);
            } else {
              throw new RetryableException(e);
            }
          }
        } else {
          // Not a partitioned bucket - not found is not found
          throw new IdNotFoundException(objectId);
        }
      } else {
        // some other exception rather than a 404
        if (e.isRetryable()) {
          throw new RetryableException(e);
        } else {
          throw new IdNotFoundException(objectId);
        }
      }
    }
  }

  private S3FetchedObject fetchObject(String bucketName, String objectMetaKey) {
    // Perform actual retrieval of object from S3/ObjectStore
    val request = new GetObjectRequest(bucketName, objectMetaKey);
    return new S3FetchedObject(s3Client.getObject(request));
  }

  private void fillPartUrls(
      ObjectKey objectKey, List<Part> parts, boolean isRelocated, boolean forExternalUse) {
    // Construct pre-signed URL's for data objects (the /data bucket)
    val expirationDate = getExpirationDate();

    for (val part : parts) {
      if (forExternalUse) {
        // There should only be one part - don't include RANGE header in pre-signed URL
        part.setUrl(
            urlGenerator.getDownloadUrl(
                bucketNamingService.getObjectBucketName(objectKey.getObjectId(), isRelocated),
                objectKey,
                expirationDate));
      } else {
        part.setUrl(
            urlGenerator.getDownloadPartUrl(
                bucketNamingService.getObjectBucketName(objectKey.getObjectId(), isRelocated),
                objectKey,
                part,
                expirationDate));
      }
    }
  }

  private Date getExpirationDate() {
    val now = LocalDateTime.now();
    return Date.from(now.plusDays(expiration).atZone(ZoneId.systemDefault()).toInstant());
  }
}
