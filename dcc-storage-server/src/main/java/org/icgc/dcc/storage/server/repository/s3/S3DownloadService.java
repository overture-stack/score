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
package org.icgc.dcc.storage.server.repository.s3;

import static com.google.common.base.Preconditions.checkArgument;

import lombok.Cleanup;
import lombok.Setter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.icgc.dcc.storage.core.model.ObjectKey;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.core.util.ObjectKeys;
import org.icgc.dcc.storage.server.exception.IdNotFoundException;
import org.icgc.dcc.storage.server.exception.InternalUnrecoverableError;
import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.icgc.dcc.storage.server.exception.RetryableException;
import org.icgc.dcc.storage.server.repository.BucketNamingService;
import org.icgc.dcc.storage.server.repository.DownloadService;
import org.icgc.dcc.storage.server.repository.PartCalculator;
import org.icgc.dcc.storage.server.repository.URLGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * service responsible for object download (full or partial)
 */
@Slf4j
@Setter
@Service
@Profile({ "aws", "collaboratory", "default" })
public class S3DownloadService implements DownloadService {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Configuration.
   */
  @Value("${collaboratory.data.directory}")
  private String dataDir;
  @Value("${collaboratory.download.expiration}")
  private int expiration;
  @Value("${object.sentinel}")
  private String sentinelObjectId;

  /**
   * Dependencies.
   */
  @Autowired
  private AmazonS3 s3Client;
  @Autowired
  private BucketNamingService bucketNamingService;
  @Autowired
  private URLGenerator urlGenerator;
  @Autowired
  private PartCalculator partCalculator;

  @Override
  public ObjectSpecification download(String objectId, long offset, long length, boolean forExternalUse) {
    try {
      checkArgument(offset > -1L);

      // Retrieve our meta file for object id
      val objectSpec = getSpecification(objectId);

      // Short-circuit in default case
      if (!forExternalUse && (offset == 0L && length < 0L)) {
        return objectSpec;
      }

      // Calculate range values
      // To retrieve to the end of the file
      if (!forExternalUse && (length < 0L)) {
        length = objectSpec.getObjectSize() - offset;
      }

      // Validate offset and length parameters:
      // Check if the offset + length > length - that would be too big
      if ((offset + length) > objectSpec.getObjectSize()) {
        throw new InternalUnrecoverableError("Specified parameters exceed object size (object id: " + objectId
            + ", offset: " + offset
            + ", length: " + length + ")");
      }

      // Construct ObjectSpecification for actual object in /data logical folder
      val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);

      List<Part> parts;
      if (forExternalUse) {
        // Return as a single part - no matter how large
        parts = partCalculator.specify(0L, -1L);
      } else {
        parts = partCalculator.divide(offset, length);
      }

      fillPartUrls(objectKey, parts, objectSpec.isRelocated(), forExternalUse);

      return new ObjectSpecification(objectKey.getKey(), objectId, objectId, parts, length, objectSpec.getObjectMd5(),
          objectSpec.isRelocated());
    } catch (Exception e) {
      log.error("Failed to download objectId: {}, offset: {}, length: {}, forExternalUse: {}: {} ",
          objectId, offset, length, forExternalUse, e);

      throw e;
    }
  }

  // This really is a misleading method name - should be retrieveMetaFile() or something
  public ObjectSpecification getSpecification(String objectId) {
    val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
    val objectMetaKey = ObjectKeys.getObjectMetaKey(dataDir, objectId);
    log.debug("Getting specification for objectId: {}, objectKey: {}, objectMetaKey: {}", objectId, objectKey,
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
      log.error("Error reading specification for objectId: {}, objectMetaKey: {}, objectKey: {}: {}",
          objectId, objectMetaKey, objectKey, e);
      throw new NotRetryableException(e);
    } catch (IOException e) {
      log.error("Failed to get specification for objectId: {}, objectMetaKey: {}, objectKey: {}: {}",
          objectId, objectMetaKey, objectKey, e);
      throw new NotRetryableException(e);
    }
  }

  private ObjectSpecification readSpecification(S3Object obj)
      throws JsonParseException, JsonMappingException, IOException {
    @Cleanup
    val inputStream = obj.getObjectContent();
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
      throw new NotRetryableException(new IllegalArgumentException("Sentinel object id not defined"));
    }
    val now = LocalDateTime.now();
    val expirationDate = Date.from(now.plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant());

    return urlGenerator.getDownloadUrl(
        bucketNamingService.getObjectBucketName("", true), ObjectKeys.getObjectKey(dataDir, sentinelObjectId),
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
          log.warn("Object with objectId: {} not found in {}, objectKey: {}: {}. Trying master bucket {}",
              objectId, stateBucketName, objectMetaKey, e, bucketNamingService.getBaseStateBucketName());
          try {
            stateBucketName = bucketNamingService.getBaseStateBucketName(); // use base bucket name
            val obj = fetchObject(stateBucketName, objectMetaKey);
            obj.setRelocated(true);
            return obj;
          } catch (AmazonServiceException e2) {
            log.error("Failed to get object with objectId: {} from {}, objectKey: {}: {}", objectId, stateBucketName,
                objectMetaKey, e);
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

  private void fillPartUrls(ObjectKey objectKey, List<Part> parts, boolean isRelocated, boolean forExternalUse) {
    // Construct pre-signed URL's for data objects (the /data bucket)
    val expirationDate = getExpirationDate();

    for (val part : parts) {
      if (forExternalUse) {
        // There should only be one part - don't include RANGE header in pre-signed URL
        part.setUrl(urlGenerator.getDownloadUrl(
            bucketNamingService.getObjectBucketName(objectKey.getObjectId(), isRelocated),
            objectKey,
            expirationDate));
      } else {
        part.setUrl(urlGenerator.getDownloadPartUrl(
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
