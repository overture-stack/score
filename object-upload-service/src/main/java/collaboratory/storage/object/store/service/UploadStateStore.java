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
package collaboratory.storage.object.store.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import collaboratory.storage.object.store.core.model.CompletedPart;
import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.store.exception.IdNotFoundException;
import collaboratory.storage.object.store.exception.InternalUnrecoverableError;
import collaboratory.storage.object.store.exception.NotRetryableException;
import collaboratory.storage.object.store.exception.RetryableException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest.KeyVersion;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;

@Slf4j
public class UploadStateStore {

  private static final String UPLOAD_SEPARATOR = "_";
  private static final String DIRECTORY_SEPARATOR = "/";
  private static final String META = ".meta";
  private static final String PART = "part";
  private static final Integer MAX_KEYS = 5000;

  @Autowired
  private AmazonS3 s3Client;

  @Value("${collaboratory.bucket.name}")
  private String bucketName;

  @Value("${collaboratory.upload.directory}")
  private String upload;

  public void create(UploadSpecification spec) {
    log.debug("Upload Specification : {}", spec);
    ObjectMapper mapper = new ObjectMapper();
    try {
      byte[] content = mapper.writeValueAsBytes(spec);
      ObjectMetadata meta = new ObjectMetadata();
      meta.setContentLength(content.length);
      s3Client.putObject(bucketName, getUploadStateKey(spec.getObjectId(), spec.getUploadId(), META),
          new ByteArrayInputStream(content), meta);
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    } catch (JsonParseException | JsonMappingException e) {
      throw new NotRetryableException(e);
    } catch (IOException e) {
      log.error("Fail to create meta file", e);
      throw new NotRetryableException(e);
    }
  }

  @SneakyThrows
  public UploadSpecification loadUploadSpecification(String objectId, String uploadId) {
    try {
      GetObjectRequest req =
          new GetObjectRequest(bucketName, getUploadStateKey(objectId,
              uploadId, META));
      S3Object obj = s3Client.getObject(req);
      ObjectMapper mapper = new ObjectMapper();

      try (S3ObjectInputStream inputStream = obj.getObjectContent()) {
        return mapper.readValue(inputStream, UploadSpecification.class);
      }
    } catch (AmazonServiceException e) {
      if (e.isRetryable()) {
        throw new RetryableException(e);
      } else {
        throw new IdNotFoundException(uploadId);
      }
    } catch (JsonParseException | JsonMappingException e) {
      throw new NotRetryableException(e);
    }

  }

  private boolean isMetaAvailable(String objectId, String uploadId) {
    try {
      s3Client.getObjectMetadata(bucketName,
          getUploadStateKey(objectId, uploadId, META));
    } catch (AmazonS3Exception ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        return false;
      }
      throw new RetryableException(ex);
    }
    return true;
  }

  @SneakyThrows
  public void markCompletedParts(String objectId, String uploadId, List<Part> parts) {
    if (parts == null || parts.size() == 0) return;
    try {
      ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
          .withBucketName(bucketName)
          .withMaxKeys(MAX_KEYS)
          .withPrefix(getUploadStateKey(objectId, uploadId, PART));
      ObjectMapper mapper = new ObjectMapper();
      ObjectListing objectListing = null;
      Collections.sort(parts, new Comparator<Part>() {

        @Override
        public int compare(Part p1, Part p2) {
          return p1.getPartNumber() - p2.getPartNumber();
        }
      });

      Iterator<Part> partItr = parts.iterator();
      do {
        objectListing = s3Client.listObjects(listObjectsRequest);
        Part part = null;
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
          String json = extractJson(objectSummary.getKey(), objectId, uploadId);
          CompletedPart completedPart = mapper.readValue(json, CompletedPart.class);
          do {
            if (partItr.hasNext()) {
              part = partItr.next();
            } else {
              return;
            }
          } while (completedPart.getPartNumber() != part.getPartNumber());
          part.setMd5(completedPart.getMd5());
        }
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    } catch (JsonParseException | JsonMappingException e) {
      throw new NotRetryableException(e);
    }
  }

  public boolean isCompleted(String objectId, String uploadId) {
    UploadSpecification spec = loadUploadSpecification(objectId, uploadId);
    Collections.sort(spec.getParts(), new Comparator<Part>() {

      @Override
      public int compare(Part p1, Part p2) {
        return p1.getPartNumber() - p2.getPartNumber();
      }
    });

    ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withMaxKeys(MAX_KEYS)
        .withPrefix(getUploadStateKey(objectId, uploadId, PART));
    ObjectListing objectListing;
    Iterator<Part> partIterator = spec.getParts().iterator();

    if (partIterator.hasNext()) {
      Part part = partIterator.next();
      do {
        objectListing = s3Client.listObjects(listObjectsRequest);
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
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
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
      return false;
    }
    return true;
  }

  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String eTag)
  {
    ObjectMapper mapper = new ObjectMapper();
    try {
      log.debug("Finalize part for object id: {}, upload id: {}, md5: {}, eTag: {}", objectId, uploadId, md5, eTag);
      String json = mapper.writeValueAsString(new CompletedPart(partNumber, md5, eTag));
      ObjectMetadata meta = new ObjectMetadata();
      meta.setContentLength(0);
      s3Client.putObject(bucketName,
          getUploadStateKey(objectId, uploadId, getLexicographicalOrderUploadPartName(partNumber, json)),
          new ByteArrayInputStream(new byte[0]), meta);
    } catch (AmazonServiceException e) {
      log.error("Storage failed", e);
      throw new RetryableException(e);
    } catch (JsonParseException | JsonMappingException e) {
      throw new NotRetryableException(e);
    } catch (IOException e) {
      log.error("Fail to finalize part", e);
      throw new InternalUnrecoverableError();
    }

  }

  @SneakyThrows
  public List<PartETag> getUploadStatePartEtags(String objectId, String uploadId) {
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withMaxKeys(MAX_KEYS)
        .withPrefix(getUploadStateKey(objectId, uploadId, PART));
    ObjectMapper mapper = new ObjectMapper();
    ObjectListing objectListing;
    List<PartETag> etags = Lists.newArrayList();
    try {
      do {
        objectListing = s3Client.listObjects(listObjectsRequest);
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
          String json = extractJson(objectSummary.getKey(), objectId, uploadId);
          CompletedPart part = mapper.readValue(json, CompletedPart.class);
          etags.add(new PartETag(part.getPartNumber(), part.getEtag()));
        }
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
      return etags;
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    } catch (JsonParseException | JsonMappingException e) {
      throw new NotRetryableException(e);
    }
  }

  public void delete(String objectId, String uploadId) {
    s3Client.deleteObject(bucketName, getUploadStateKey(objectId, uploadId, META));
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withPrefix(getUploadStateKey(objectId, uploadId, PART));
    ObjectListing objectListing;
    try {
      do {
        objectListing = s3Client.listObjects(listObjectsRequest);
        Builder<KeyVersion> keys = ImmutableList.builder();
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
          keys.add(new KeyVersion(objectSummary.getKey()));
        }
        DeleteObjectsRequest deletes = new DeleteObjectsRequest(bucketName).withKeys(keys.build());
        if (!deletes.getKeys().isEmpty()) {
          s3Client.deleteObjects(deletes);
        }
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
      s3Client.deleteObject(bucketName, getUploadStateKey(objectId, uploadId));
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    }
  }

  public String getUploadId(String objectId) {
    ListObjectsRequest req = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withMaxKeys(MAX_KEYS)
        .withDelimiter(DIRECTORY_SEPARATOR)
        .withPrefix(getUploadStateKey(objectId, ""));
    try {
      ObjectListing objectListing;
      do {
        objectListing = s3Client.listObjects(req);
        for (String prefix : objectListing.getCommonPrefixes()) {
          log.debug("Found object upload key: {}", prefix);
          String uploadId = getUploadIdFromMeta(objectId, prefix);
          if (isMetaAvailable(objectId, uploadId)) {
            return uploadId;
          }
        }
        req.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    }
    log.warn("Upload Id not found for object ID: {}", objectId);
    throw new IdNotFoundException("Upload ID not found for object ID: " + objectId);
  }

  private String getUploadIdFromMeta(String objectId, String objectUploadKey) {
    return StringUtils.removeEnd(StringUtils.removeStart(objectUploadKey, getUploadStateKey(objectId, "")),
        DIRECTORY_SEPARATOR);
  }

  private String getUploadStateKey(String objectId, String uploadId) {
    return new StringBuilder(upload)
        .append(DIRECTORY_SEPARATOR)
        .append(objectId)
        .append(UPLOAD_SEPARATOR)
        .append(uploadId)
        .toString();
  }

  private String getUploadStateKey(String objectId, String uploadId, String filename) {
    return new StringBuilder(upload)
        .append(DIRECTORY_SEPARATOR)
        .append(objectId)
        .append(UPLOAD_SEPARATOR)
        .append(uploadId)
        .append(DIRECTORY_SEPARATOR)
        .append(filename)
        .toString();
  }

  private String getLexicographicalOrderUploadPartName(int partNumber, String json) {
    return String.format("%s-%08x|%s", PART, (0xFFFFFFFF & partNumber), json);
  }

  private String extractJson(String key, String objectId, String uploadId, int partNumber) {
    return StringUtils.removeStart(key,
        getUploadStateKey(objectId, uploadId, String.format("%s-%08X|", PART, (0xFFFFFFFF & partNumber))));
  }

  private int extractPartNumber(String objectId, String uploadId, String partKey) {
    String hexNumber =
        StringUtils.substringBetween(StringUtils.removeStart(partKey, getUploadStateKey(objectId, uploadId)), PART
            + "-", "|");
    return Integer.parseInt(hexNumber, 16);
  }

  private String extractJson(String key, String objectId, String uploadId) {
    return StringUtils.substringAfter(StringUtils.removeStart(key, getUploadStateKey(objectId, uploadId)), "|");
  }

  public void deleletePart(String objectId, String uploadId, int partNumber) {
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withPrefix(getUploadStateKey(objectId, uploadId, getLexicographicalOrderUploadPartName(partNumber, "")));
    try {
      ObjectListing objectListing;
      do {
        objectListing = s3Client.listObjects(listObjectsRequest);
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
          s3Client.deleteObject(bucketName, objectSummary.getKey());
        }
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    }
  }
}
