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
import java.util.List;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
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

  private boolean isUploadPartCompleted(String objectId, String uploadId, int partNumber) {
    try {
      s3Client.getObjectMetadata(bucketName,
          getUploadStateKey(objectId, uploadId, getLexicographicalOrderUploadPartName(partNumber)));
    } catch (AmazonS3Exception ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        return false;
      }
      throw new RetryableException(ex);
    }
    return true;
  }

  @SneakyThrows
  public List<CompletedPart> retrieveCompletedParts(String objectId, String uploadId) {

    try {
      ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
          .withBucketName(bucketName)
          .withPrefix(getUploadStateKey(objectId, uploadId, PART));
      ObjectMapper mapper = new ObjectMapper();
      ObjectListing objectListing;
      Builder<CompletedPart> completedParts = ImmutableList.builder();
      do {
        objectListing = s3Client.listObjects(listObjectsRequest);
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
          S3Object obj = s3Client.getObject(objectSummary.getBucketName(), objectSummary.getKey());
          try (S3ObjectInputStream inputStream = obj.getObjectContent()) {
            CompletedPart part = mapper.readValue(inputStream, CompletedPart.class);
            completedParts.add(part);
          }
        }
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());

      return completedParts.build();
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    }

  }

  @SneakyThrows
  public List<Part> retrieveIncompletedParts(String objectId, String uploadId, boolean isCompleted) {
    UploadSpecification spec = loadUploadSpecification(objectId, uploadId);
    Builder<Part> incompletedParts = ImmutableList.builder();
    for (Part part : spec.getParts()) {
      if (isUploadPartCompleted(objectId, uploadId, part.getPartNumber()) == isCompleted) {
        incompletedParts.add(part);
      }
    }
    return incompletedParts.build();
  }

  public boolean isCompleted(String objectId, String uploadId) {
    UploadSpecification spec = loadUploadSpecification(objectId, uploadId);
    for (Part part : spec.getParts()) {
      if (!isUploadPartCompleted(objectId, uploadId, part.getPartNumber())) {
        return false;
      }
    }
    return true;
  }

  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String eTag)
  {
    ObjectMapper mapper = new ObjectMapper();
    try {
      log.debug("Finalize part for object id: {}, upload id: {}, md5: {}, eTag: {}", objectId, uploadId, md5, eTag);
      byte[] content = mapper.writeValueAsBytes(new CompletedPart(partNumber, md5, eTag));
      ObjectMetadata meta = new ObjectMetadata();
      meta.setContentLength(content.length);
      s3Client.putObject(bucketName,
          getUploadStateKey(objectId, uploadId, getLexicographicalOrderUploadPartName(partNumber)),
          new ByteArrayInputStream(content), meta);
    } catch (AmazonServiceException e) {
      log.error("Storage failed", e);
      throw new RetryableException(e);

    } catch (IOException e) {
      log.error("Fail to finalize part", e);
      throw new InternalUnrecoverableError();
    }

  }

  @SneakyThrows
  public List<PartETag> getUploadStatePartEtags(String objectId, String uploadId) {
    ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
        .withBucketName(bucketName)
        .withPrefix(getUploadStateKey(objectId, uploadId, PART));
    ObjectMapper mapper = new ObjectMapper();
    ObjectListing objectListing;
    List<PartETag> etags = Lists.newArrayList();
    try {
      do {
        objectListing = s3Client.listObjects(listObjectsRequest);
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
          log.debug("ETag: {}", objectSummary.getETag());
          S3Object obj = s3Client.getObject(objectSummary.getBucketName(), objectSummary.getKey());
          try (S3ObjectInputStream inputStream = obj.getObjectContent()) {
            CompletedPart part = mapper.readValue(inputStream, CompletedPart.class);
            etags.add(new PartETag(part.getPartNumber(), part.getEtag()));
          }
        }
        listObjectsRequest.setMarker(objectListing.getNextMarker());
      } while (objectListing.isTruncated());
      return etags;
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
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
        .withPrefix(getUploadStateKey(objectId, ""));

    try {
      ObjectListing objectListing;
      do {
        objectListing = s3Client.listObjects(req);
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
          log.debug("Found object upload key: {}", objectSummary.getKey());
          String uploadId = getUploadIdFromMeta(objectId, objectSummary.getKey());
          // TODO: look for .meta
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
        DIRECTORY_SEPARATOR + META);
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

  private String getLexicographicalOrderUploadPartName(int partNumber) {
    return String.format("%s-%08x", PART, (0xFFFFFFFF & partNumber));
  }

  public void deleletePart(String objectId, String uploadId, int partNumber) {
    try {
      s3Client.deleteObject(bucketName,
          getUploadStateKey(objectId, uploadId, getLexicographicalOrderUploadPartName(partNumber)));
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    }
  }
}
