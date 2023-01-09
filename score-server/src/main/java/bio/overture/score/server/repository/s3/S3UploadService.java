package bio.overture.score.server.repository.s3;

import bio.overture.score.core.model.ObjectKey;
import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.core.model.UploadProgress;
import bio.overture.score.core.util.ObjectKeys;
import bio.overture.score.server.config.S3Config;
import bio.overture.score.server.exception.IdNotFoundException;
import bio.overture.score.server.exception.InternalUnrecoverableError;
import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.exception.RetryableException;
import bio.overture.score.server.metadata.MetadataEntity;
import bio.overture.score.server.metadata.MetadataService;
import bio.overture.score.core.util.PartCalculator;
import bio.overture.score.server.repository.URLGenerator;
import bio.overture.score.server.repository.UploadPartDetail;
import bio.overture.score.server.repository.UploadService;
import bio.overture.score.server.repository.UploadStateStore;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static bio.overture.score.server.metadata.MetadataService.getAnalysisId;

/**
 * A service for object upload.
 */
@Slf4j
@Setter
@Service
@Profile({ "aws", "collaboratory", "default" })
public class S3UploadService implements UploadService {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String UNPUBLISHED_ANALYSIS_STATE = "UNPUBLISHED";

  /**
   * Configuration.
   */
  @Value("${collaboratory.data.directory}")
  private String dataDir;
  @Value("${collaboratory.upload.expiration}")
  private int expiration;

  @Value("${metadata.useLegacyMode:false}")
  private boolean useLegacyMode;

  @Autowired
  private S3Config s3Conf;

  /**
   * Dependencies.
   */
  @Autowired
  private AmazonS3 s3Client;
  @Autowired
  private MetadataService metadataClient;
  @Autowired
  private S3BucketNamingService bucketNamingService;
  @Autowired
  private UploadStateStore stateStore;
  @Autowired
  private URLGenerator urlGenerator;
  @Autowired
  private PartCalculator partCalculator;

  @Override
  public ObjectSpecification initiateUpload(String objectId, long fileSize, String md5, boolean overwrite) {
    // First ensure that the system is aware of the requested object
    checkRegistered(objectId);

    val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
    log.debug("Initiating upload for object key: {}, overwrite: {}", objectKey, overwrite);

    // If we don't want to overwrite
    if (!overwrite) {
      if (exists(objectId)) {
        val message = String.format("Attempted to overwrite object id %s", objectId);
        log.error(message); // Log overwrite attempt occurrence to audit log file
        throw new InternalUnrecoverableError(message);
      }
    }

    // But if we want to force the upload, check if the object exists already
    try {
      val uploadId = getUploadId(objectId);
      stateStore.delete(objectId, uploadId);
    } catch (IdNotFoundException e) {
      log.info("Upload id not found. Initiating new upload...");
    }

    val actualBucketName = bucketNamingService.getObjectBucketName(objectId);

    val request = new InitiateMultipartUploadRequest(actualBucketName, objectKey.getKey());
    try {
      s3Conf.encrypt(request);

      // Initiates a multipart upload and returns an InitiateMultipartUploadResult which contains an upload ID.
      // This upload ID associates all the parts in the specific upload and is used in each of your subsequent
      // uploadPart(UploadPartRequest) requests. You also include this upload ID in the final request to either
      // complete, or abort the multipart upload request.
      val result = s3Client.initiateMultipartUpload(request);
      val parts = partCalculator.divide(fileSize);

      val now = LocalDateTime.now();
      val expirationDate = Date.from(now.plusDays(expiration).atZone(ZoneId.systemDefault()).toInstant());
      for (val part : parts) {
        part.setUrl(urlGenerator.getUploadPartUrl(actualBucketName, objectKey, result.getUploadId(), part,
            expirationDate));
      }

      val spec = new ObjectSpecification(objectKey.getKey(), objectId, result.getUploadId(), parts, fileSize, md5, false);

      // Write out .meta file
      stateStore.create(spec);
      return spec;
    } catch (AmazonServiceException e) {
      log.error("Multipart Upload Initialization failure", e);
      if (e.getErrorCode().equals("KMS.DisabledException")) {
        throw new InternalUnrecoverableError(e);
      }

      throw new RetryableException(e);
    }
  }

  @Override
  public boolean exists(@NonNull String objectId) {
    val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
    String actualBucketName = bucketNamingService.getStateBucketName(objectId);
    try {
      s3Client.getObjectMetadata(actualBucketName, objectKey.getMetaKey());
      return true;
    } catch (AmazonServiceException e) {

      if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
        if (bucketNamingService.isPartitioned()) {

          // Try again with master bucket
          log.info("Metafile {} not found in {}. Trying master bucket {}",
              objectKey.getMetaKey(), actualBucketName, bucketNamingService.getBaseStateBucketName());
          try {
            actualBucketName = bucketNamingService.getBaseStateBucketName(); // use base bucket name
            s3Client.getObjectMetadata(actualBucketName, objectKey.getMetaKey());
            log.info("ObjectKey {} found in master bucket {}", objectKey, actualBucketName);
            return true;
          } catch (AmazonServiceException e2) {
            log.info("ObjectKey {} also not found in master bucket {}", objectKey, actualBucketName);
            return false;
          }
        } else {
          // Not a partitioned bucket - not found is not found
        }
      } else if (e.isRetryable()) {
        // Don't depend on Amazon's isRetryable() flag...
        // (we originally defaulted to retry everything)
        throw new RetryableException(e);
      } else {
        // ...but figure if they say it's not retryable, it's really not
        throw new NotRetryableException(e);
      }
    }
    return false;
  }

  private boolean isPartExists(@NonNull ObjectKey objectKey, String uploadId, int partNumber, String eTag) {
    List<PartSummary> parts = null;
    val objectId = objectKey.getObjectId();
    val actualBucketName = bucketNamingService.getObjectBucketName(objectId);
    try {
        val req = new ListPartsRequest(actualBucketName, objectKey.getKey(), uploadId);
        req.setPartNumberMarker(partNumber - 1);
        req.setMaxParts(1);
        parts = s3Client.listParts(req).getParts();
    } catch (RestClientException | AmazonClientException e) {
      log.error(
          "Request failure checking for part existence with objectKey: {}, uploadId: {}, partNumber: {}, eTag: {}: ",
          objectKey, uploadId, partNumber, eTag, e);
      throw new RetryableException(e);
    } catch (Exception e) {
      log.error(
          "Unknown failure checking for part existence with objectKey: {}, uploadId: {}, partNumber: {}, eTag: {}: ",
          objectKey, uploadId, partNumber, eTag, e);
      throw new NotRetryableException(e);
    }

    if (parts != null && parts.size() != 0) {
      val part = parts.get(0);
      if (part.getPartNumber() == partNumber && part.getETag().equals(eTag)) {
        return true;
      }
    }

    return false;
  }

  @Override
  @SneakyThrows
  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String eTag) {
    if (md5 != null && eTag != null && !md5.isEmpty() && !eTag.isEmpty()) {
      if (isPartExists(ObjectKeys.getObjectKey(dataDir, objectId), uploadId, partNumber, eTag)) {
        stateStore.finalizeUploadPart(objectId, uploadId, partNumber, md5, eTag);
      } else {
        val message = String.format("Part does not exist with number %s for objectId %s and uploadId %s",
            partNumber, objectId, uploadId);
        throw new NotRetryableException(new IOException(message));
      }
    } else {
      val message =
          String.format("Invalid etag for part with number %s does not exist for objectId %s and uploadId %s",
              partNumber, objectId, uploadId);

      throw new NotRetryableException(new IOException(message));
    }
  }

  @Override
  public void finalizeUpload(String objectId, String uploadId) {
    log.info("finalizing object id {} with upload id: {}", objectId, uploadId);

    val actualBucketName = bucketNamingService.getObjectBucketName(objectId);
    val actualStateBucketName = bucketNamingService.getStateBucketName(objectId);

    if (stateStore.isCompleted(objectId, uploadId)) {
      try {
        val details = stateStore.getUploadStatePartDetails(objectId, uploadId);
        val etags = details.values().stream().map(detail -> detail.getEtag()).collect(Collectors.toList());
        val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
        val request = new CompleteMultipartUploadRequest(actualBucketName, objectKey.getKey(), uploadId, etags);

        try {
          s3Client.completeMultipartUpload(request);
        } catch (AmazonS3Exception e) {
          if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
            log.warn("Object keys don't exist for completing requested Multipart upload." +
                    "Assuming it is completed as part of an earlier request.");
            return; // if object keys don't exist - it means it is already finalized, no need to proceed further
          }

          log.error("Error completing multipart upload for for objectId {} and uploadId {}", objectId, uploadId);
          throw e;
        }


        val spec = stateStore.read(objectId, uploadId);
        // Update meta with md5's
        spec.getParts().forEach(part -> {
          UploadPartDetail detail = details.get(part.getPartNumber());
          part.setSourceMd5(detail != null ? detail.getMd5() : "<missing>");
        });

        byte[] content = MAPPER.writeValueAsBytes(spec);
        val data = new ByteArrayInputStream(content);
        val meta = new ObjectMetadata();
        meta.setContentLength(content.length);
        val objectMetaKey = ObjectKeys.getObjectMetaKey(dataDir, objectId);
        log.debug("about to s3.putObject into " + actualStateBucketName + ": " + objectMetaKey.toString());
        s3Client.putObject(actualStateBucketName, objectMetaKey, data, meta);
        // Delete working files in upload directory
        log.debug("About to delete working files from state directory");
        stateStore.delete(objectId, uploadId);
        log.debug("Upload for {} (upload id {}) finalized", objectId, uploadId);
      } catch (AmazonServiceException e) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
          log.warn("Key doesn't exist. Assuming it was deleted.");
          return; // if object keys not found during any step in finalization - assume it is deleted and ignore the error
        }
        log.error("Service problem with objectId: {}, uploadId: {}", objectId, uploadId, e);
        throw new RetryableException(e);
      } catch (IOException e) {
        log.error("Serialization problem with objectId: {}, uploadId: {}", objectId, uploadId, e);
        throw new InternalUnrecoverableError(e);
      }
    } else {
      log.error("Upload cannot be finalized because it is not completed.");
      throw new NotRetryableException(new IOException("Object cannot be finalized"));
    }
  }

  @Override
  public String getUploadId(String objectId) {
    return stateStore.getUploadId(objectId);
  }

  @Override
  public ObjectMetadata getObjectMetadata(String objectId) {
    try {
      val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
      return s3Client.getObjectMetadata(bucketNamingService.getStateBucketName(objectId), objectKey.getMetaKey());
    } catch (AmazonServiceException e) {
      log.error("Unable to retrieve object metadata for object id: {}", objectId, e);
      throw new NotRetryableException(e);
    }
  }

  @Override
  public UploadProgress getUploadStatus(String objectId, String uploadId, long fileSize) {
    val spec = stateStore.read(objectId, uploadId);
    val validSpec = spec.getObjectSize() == fileSize;
    if (validSpec) {
      stateStore.markCompletedParts(objectId, uploadId, spec.getParts());
      return new UploadProgress(objectId, uploadId, spec.getParts());
    }

    val msg =
        String
            .format(
                "Error getting upload status for objectId %s with uploadId %s: fileSize %d does not match registered object size %d",
                objectId, uploadId, fileSize, spec.getObjectSize());
    log.error(msg);
    throw new NotRetryableException(new IllegalStateException(msg));
  }

  @Override
  public void cancelUploads() {
    try {
      for (val upload : listUploads()) {
        val uploadId = upload.getUploadId();
        val objectKey = upload.getKey();
        val objectId = ObjectKeys.getObjectId(dataDir, objectKey);
        val request =
            new AbortMultipartUploadRequest(bucketNamingService.getObjectBucketName(objectId), objectKey, uploadId);

        s3Client.abortMultipartUpload(request);
        stateStore.delete(objectId, uploadId);
      }
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    }
  }

  @Override
  public void cancelUpload(String objectId, String uploadId) {
    try {
      val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
      val request =
          new AbortMultipartUploadRequest(bucketNamingService.getObjectBucketName(objectId), objectKey.getKey(),
              uploadId);

      s3Client.abortMultipartUpload(request);
      stateStore.delete(objectId, uploadId);
    } catch (AmazonServiceException e) {
      log.error("Failed to cancel upload for objectId: {}, uploadId: {}: ", objectId, uploadId, e);
      throw new RetryableException(e);
    }
  }

  @Override
  public void recover(String objectId, long fileSize) {
    val uploadId = getUploadId(objectId);
    val spec = stateStore.read(objectId, uploadId);
    val objectSize = spec.getObjectSize();

    val changed = fileSize != objectSize;
    if (changed) {
      log.error("Failed to recover objectId: {}, fileSize: {} because its size has changed", objectId, fileSize);
      throw new NotRetryableException();
    }
  }

  @Override
  public void deletePart(String objectId, String uploadId, int partNumber) {
    log.info("Deleting part with number {} for objectId: {}, uploadId: {}", partNumber, objectId, uploadId);
    stateStore.deletePart(objectId, uploadId, partNumber);
  }

  void checkRegistered(String objectId) {
    val entity = metadataClient.getEntity(objectId);
    if (!entity.getId().equals(objectId)) {
      val message = String.format("Critical Error: checked for objectId %s and metadata server returned %s as match",
          objectId, entity.getId());

      log.error(message); // Log to audit log file
      throw new InternalUnrecoverableError(message);
    }

    if (!useLegacyMode){
      checkUnpublishedAnalysisState(entity);
    }
  }

  void checkUnpublishedAnalysisState(MetadataEntity entity){
    val objectId = entity.getId();
    val analysisState = metadataClient.getAnalysisStateForMetadata(entity);
    if (!analysisState.equals(UNPUBLISHED_ANALYSIS_STATE)){
      val message = String.format("Critical Error: cannot complete upload for objectId '%s' with "
              + "analysisState '%s' and analysisId '%s'. "
              + "Can only upload objects that have the analysisState '%s'. Update the file metadata and retry.",
          objectId, analysisState, getAnalysisId(entity), UNPUBLISHED_ANALYSIS_STATE);
      log.error(message); // Log to audit log file
      throw new NotRetryableException(new IllegalStateException(message));
    }
  }

  @Override
  public List<MultipartUpload> listUploads() {
    List<MultipartUpload> result = null;
    try {
      if (bucketNamingService.isPartitioned()) {
        result = new ArrayList<MultipartUpload>();
        for (int i = 0; i < bucketNamingService.getBucketPoolSize(); i++) {
          val actualBucketName =
              bucketNamingService.constructBucketName(bucketNamingService.getBaseObjectBucketName(), i);
          val request = new ListMultipartUploadsRequest(actualBucketName);
          val response = s3Client.listMultipartUploads(request);
          result.addAll(response.getMultipartUploads());
        }

      } else {
        val request = new ListMultipartUploadsRequest(bucketNamingService.getBaseObjectBucketName());
        val response = s3Client.listMultipartUploads(request);
        result = response.getMultipartUploads();
      }

      return result;
    } catch (AmazonServiceException e) {
      log.error("Failed to list uploads on partition : ", e);
      throw new RetryableException(e);
    }
  }

}
