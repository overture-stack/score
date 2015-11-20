/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.storage.server.service.upload;

import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBetween;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.storage.core.model.CompletedPart;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.server.exception.IdNotFoundException;
import org.icgc.dcc.storage.server.exception.InternalUnrecoverableError;
import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.icgc.dcc.storage.server.exception.RetryableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Lists;

import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Stores and retrieves the state of a upload's progress.
 */
@Slf4j
@Setter
public class UploadStateStore {

  /**
   * Constants.
   */
  private static final String UPLOAD_SEPARATOR = "_";
  private static final String DIRECTORY_SEPARATOR = "/";
  private static final String META = ".meta";
  private static final String PART = "part";
  private static final Integer MAX_KEYS = 5000;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Configuration.
   */
  @Value("${collaboratory.bucket.name}")
  private String bucketName;
  @Value("${collaboratory.upload.directory}")
  private String uploadDir;

  /**
   * Dependencies.
   */
  @Autowired
  private AmazonS3 s3Client;

  /**
   * Store the upload specification.
   */
  public void create(@NonNull ObjectSpecification spec) {
    val uploadStateKey = getUploadStateKey(spec.getObjectId(), spec.getUploadId(), META);

    try {
      byte[] content = MAPPER.writeValueAsBytes(spec);
      val data = new ByteArrayInputStream(content);
      val meta = new ObjectMetadata();
      meta.setContentLength(content.length);

      s3Client.putObject(bucketName, uploadStateKey, data, meta);
    } catch (AmazonServiceException e) {
      log.error("Failed to create meta file for spec: {}: {}", spec, e);
      throw new RetryableException(e);
    } catch (IOException e) {
      log.error("Failed to create meta file for spec: {}: {}", spec, e);
      throw new NotRetryableException(e);
    }
  }

  @SneakyThrows
  public ObjectSpecification read(String objectId, String uploadId) {
    val uploadStateKey = getUploadStateKey(objectId, uploadId, META);

    try {
      val request = new GetObjectRequest(bucketName, uploadStateKey);
      val obj = s3Client.getObject(request);

      try (val inputStream = obj.getObjectContent()) {
        return MAPPER.readValue(inputStream, ObjectSpecification.class);
      }
    } catch (AmazonServiceException e) {
      if (e.isRetryable()) {
        throw new RetryableException(e);
      } else {
        throw new IdNotFoundException(uploadId);
      }
    } catch (JsonParseException | JsonMappingException e) {
      log.error("Error reading specification for objectId {} and uploadId {}", objectId, uploadId);
      throw new NotRetryableException(e);
    }
  }

  public void delete(String objectId, String uploadId) {
    val uploadStateKey = getUploadStateKey(objectId, uploadId, META);

    s3Client.deleteObject(bucketName, uploadStateKey);
  }

  public void deletePart(String objectId, String uploadId, int partNumber) {
    val partName = formatUploadPartName(partNumber, "");
    val uploadStateKey = getUploadStateKey(objectId, uploadId, partName);

    eachObjectSummary(uploadStateKey, objectSummary -> s3Client.deleteObject(bucketName, objectSummary.getKey()));
  }

  @SneakyThrows
  public void markCompletedParts(String objectId, String uploadId, List<Part> parts) {
    if (parts == null || parts.size() == 0) {
      return;
    }

    try {
      sortPartsByNumber(parts);
      val partIterator = parts.iterator();

      val request = new ListObjectsRequest()
          .withBucketName(bucketName)
          .withMaxKeys(MAX_KEYS)
          .withPrefix(getUploadStateKey(objectId, uploadId, PART));

      ObjectListing objectListing = null;
      do {
        objectListing = s3Client.listObjects(request);
        Part part = null;
        for (val objectSummary : objectListing.getObjectSummaries()) {
          CompletedPart completedPart = readCompletedPart(objectId, uploadId, objectSummary);
          do {
            if (partIterator.hasNext()) {
              part = partIterator.next();
            } else {
              return;
            }
          } while (completedPart.getPartNumber() != part.getPartNumber());
          part.setMd5(completedPart.getMd5());
        }
        request.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
    } catch (AmazonServiceException e) {
      log.error("Failed to mark completed parts for object metadata for objectId: {}, uploadId: {}, parts: {}",
          objectId, uploadId, parts, e);
      throw new RetryableException(e);
    }
  }

  public boolean isCompleted(String objectId, String uploadId) {
    val spec = read(objectId, uploadId);

    sortPartsByNumber(spec.getParts());
    val partIterator = spec.getParts().iterator();

    val request = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withMaxKeys(MAX_KEYS)
        .withPrefix(getUploadStateKey(objectId, uploadId, PART));

    if (partIterator.hasNext()) {
      Part part = partIterator.next();

      ObjectListing objectListing;
      do {
        objectListing = s3Client.listObjects(request);
        for (val objectSummary : objectListing.getObjectSummaries()) {
          int partNumber = extractPartNumber(objectId, uploadId, objectSummary.getKey());
          if (part.getPartNumber() != partNumber) {
            return false;
          }
          if (partIterator.hasNext()) {
            part = partIterator.next();
          } else {
            return true;
          }
        }
        request.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
      return false;
    }

    return true;
  }

  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String eTag) {
    try {
      log.debug("Finalizing part for object id: {}, upload id: {}, md5: {}, eTag: {}", objectId, uploadId, md5, eTag);
      val json = MAPPER.writeValueAsString(new CompletedPart(partNumber, md5, eTag));
      val partName = formatUploadPartName(partNumber, json);

      val meta = new ObjectMetadata();
      meta.setContentLength(0);
      val data = new ByteArrayInputStream(new byte[0]);
      val uploadStateKey = getUploadStateKey(objectId, uploadId, partName);

      s3Client.putObject(bucketName, uploadStateKey, data, meta);
    } catch (AmazonServiceException e) {
      // TODO: Log args
      log.error("Storage failed", e);
      throw new RetryableException(e);
    } catch (JsonParseException | JsonMappingException e) {
      // TODO: Log
      throw new NotRetryableException(e);
    } catch (IOException e) {
      log.error("Failed to finalize upload part: {}, uploadId: {}, partNumber: {}",
          objectId, uploadId, partNumber, e);
      throw new InternalUnrecoverableError();
    }
  }

  @SneakyThrows
  public List<PartETag> getUploadStatePartEtags(String objectId, String uploadId) {
    val uploadStateKey = getUploadStateKey(objectId, uploadId, PART);
    val etags = Lists.<PartETag> newArrayList();

    eachObjectSummary(uploadStateKey, (objectSummary) -> {
      CompletedPart part = readCompletedPart(objectId, uploadId, objectSummary);
      PartETag etag = new PartETag(part.getPartNumber(), part.getEtag());

      etags.add(etag);
    });

    return etags;
  }

  public String getUploadId(String objectId) {
    val uploadStateKey = getUploadStateKey(objectId, "" /* uploadId */ );

    val request = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withMaxKeys(MAX_KEYS)
        .withDelimiter(getDirectorySeparator())
        .withPrefix(uploadStateKey);

    try {
      ObjectListing objectListing;
      do {
        objectListing = s3Client.listObjects(request);
        for (val prefix : objectListing.getCommonPrefixes()) {
          log.debug("Found object upload key: {}", prefix);
          val uploadId = getUploadIdFromMeta(objectId, prefix);

          if (isMetaAvailable(objectId, uploadId)) {
            return uploadId;
          }
        }

        request.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
    } catch (AmazonServiceException e) {
      log.error("Error getting uploadId for objectId {}", objectId);
      throw new RetryableException(e);
    }

    log.warn("Upload Id not found for object ID: {}", objectId);
    throw new IdNotFoundException("Upload ID not found for object ID: " + objectId);
  }

  @SneakyThrows
  private CompletedPart readCompletedPart(String objectId, String uploadId, S3ObjectSummary objectSummary) {
    try {
      val json = extractJson(objectSummary.getKey(), objectId, uploadId);
      return MAPPER.readValue(json, CompletedPart.class);
    } catch (JsonParseException | JsonMappingException e) {
      log.error("Failed to read completed parts for objectId: {}, uploadId: {}, objectSummary: {}: {}",
          objectId, uploadId, objectSummary.getKey(), e);
      throw new NotRetryableException(e);
    }
  }

  private void eachObjectSummary(String prefix, Consumer<S3ObjectSummary> callback) {
    val request = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withMaxKeys(MAX_KEYS)
        .withPrefix(prefix);

    try {
      ObjectListing objectListing;
      do {
        objectListing = s3Client.listObjects(request);
        for (val objectSummary : objectListing.getObjectSummaries()) {
          callback.accept(objectSummary);
        }

        request.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
    } catch (AmazonServiceException e) {
      log.error("Failed to list objects with prefix: {}: {}", prefix, e);
      throw new RetryableException(e);
    }
  }

  boolean isMetaAvailable(String objectId, String uploadId) {
    val uploadStateKey = getUploadStateKey(objectId, uploadId, META);

    try {
      // This is actually how you are supposed to test
      s3Client.getObjectMetadata(bucketName, uploadStateKey);
    } catch (AmazonS3Exception e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        return false;
      }

      log.error("Error checking meta existence for objectId {} and uploadId {}", objectId, uploadId);
      throw new RetryableException(e);
    }

    return true;
  }

  private String getUploadStateKey(String objectId, String uploadId) {
    val directorySeparator = getDirectorySeparator();

    return new StringBuilder(uploadDir)
        .append(directorySeparator)
        .append(objectId)
        .append(UPLOAD_SEPARATOR)
        .append(uploadId)
        .toString();
  }

  private String getUploadIdFromMeta(String objectId, String objectUploadKey) {
    val uploadId = "";
    val uploadStateKey = getUploadStateKey(objectId, uploadId);
    return removeEnd(removeStart(objectUploadKey, uploadStateKey), getDirectorySeparator());
  }

  private int extractPartNumber(String objectId, String uploadId, String partKey) {
    val uploadStateKey = getUploadStateKey(objectId, uploadId);
    val hexNumber = substringBetween(removeStart(partKey, uploadStateKey), PART + "-", "|");
    return Integer.parseInt(hexNumber, 16);
  }

  private String extractJson(String key, String objectId, String uploadId) {
    val uploadStateKey = getUploadStateKey(objectId, uploadId);
    return substringAfter(removeStart(key, uploadStateKey), "|");
  }

  private String getUploadStateKey(String objectId, String uploadId, String filename) {
    val directorySeparator = getDirectorySeparator();

    return new StringBuilder(uploadDir)
        .append(directorySeparator)
        .append(objectId)
        .append(UPLOAD_SEPARATOR)
        .append(uploadId)
        .append(directorySeparator)
        .append(filename)
        .toString();
  }

  static void sortPartsByNumber(List<Part> parts) {
    Collections.sort(parts, (p1, p2) -> p1.getPartNumber() - p2.getPartNumber());
  }

  static String getDirectorySeparator() {
    // https://github.com/scireum/s3ninja/issues/34
    return Boolean.getBoolean("s3ninja") ? "_" : DIRECTORY_SEPARATOR;
  }

  /**
   * Formats a part name in lexicographical order.
   */
  static String formatUploadPartName(int partNumber, String json) {
    return String.format("%s-%08x|%s", PART, (0xFFFFFFFF & partNumber), json);
  }

}
