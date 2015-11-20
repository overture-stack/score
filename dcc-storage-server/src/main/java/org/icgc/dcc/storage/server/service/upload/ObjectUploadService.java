package org.icgc.dcc.storage.server.service.upload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.UploadProgress;
import org.icgc.dcc.storage.core.util.ObjectKeys;
import org.icgc.dcc.storage.server.config.S3Config;
import org.icgc.dcc.storage.server.exception.IdNotFoundException;
import org.icgc.dcc.storage.server.exception.InternalUnrecoverableError;
import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.icgc.dcc.storage.server.exception.RetryableException;
import org.icgc.dcc.storage.server.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartSummary;
import com.amazonaws.services.s3.model.transform.Unmarshallers.ListPartsResultUnmarshaller;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A service for object upload.
 */
@Slf4j
@Setter
@Service
public class ObjectUploadService {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Configuration.
   */
  @Value("${collaboratory.bucket.name}")
  private String bucketName;
  @Value("${collaboratory.data.directory}")
  private String dataDir;
  @Value("${collaboratory.upload.expiration}")
  private int expiration;
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
  private UploadStateStore stateStore;
  @Autowired
  private ObjectURLGenerator urlGenerator;
  @Autowired
  private ObjectPartCalculator partCalculator;

  public ObjectSpecification initiateUpload(String objectId, long fileSize, boolean overwrite) {
    // First ensure that the system is aware of the requested object
    checkRegistered(objectId);

    val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
    log.debug("Initiating upload for object key: {}, overwrite: {}", objectKey, overwrite);
    if (!overwrite) {
      if (exists(objectId)) {
        val message = String.format("Attempted to overwrite object id %s", objectId);
        log.error(message); // Log overwrite attempt occurrence to audit log file
        throw new InternalUnrecoverableError(message);
      }
    }

    // Check if object exists already
    try {
      val uploadId = getUploadId(objectId);
      stateStore.delete(objectId, uploadId);
    } catch (IdNotFoundException e) {
      log.info("No upload ID found. Initiate upload...");
    }

    val request = new InitiateMultipartUploadRequest(bucketName, objectKey);
    try {
      s3Conf.encrypt(request);

      val result = s3Client.initiateMultipartUpload(request);
      val parts = partCalculator.divide(fileSize);

      val now = LocalDateTime.now();
      val expirationDate = Date.from(now.plusDays(expiration).atZone(ZoneId.systemDefault()).toInstant());
      for (val part : parts) {
        part.setUrl(urlGenerator.getUploadPartUrl(bucketName, objectKey, result.getUploadId(), part, expirationDate));
      }

      val spec = new ObjectSpecification(objectKey, objectId, result.getUploadId(), parts, fileSize);
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

  public boolean exists(String objectId) {
    val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
    try {
      s3Client.getObjectMetadata(bucketName, objectKey);

      return true;
    } catch (AmazonServiceException e) {
      if (e.getStatusCode() != HttpStatus.NOT_FOUND.value()) {
        log.error("Failure in Amazon Client when requesting object id: {} from bucket: {}", objectId, bucketName, e);
        throw new RetryableException(e);
      }

      log.info("Object key not found: {}", objectKey);
      return false;
    }
  }

  private boolean isPartExists(String objectKey, String uploadId, int partNumber, String eTag) {
    List<PartSummary> parts = null;
    try {
      if (s3Conf.getEndpoint() == null) {
        val req = new ListPartsRequest(bucketName, objectKey, uploadId);
        req.setPartNumberMarker(partNumber - 1);
        req.setMaxParts(1);
        parts = s3Client.listParts(req).getParts();
      } else {
        // HACK: Incompatible API. Serialization issue at the XML
        val request = new RestTemplate();
        val signed = new GeneratePresignedUrlRequest(bucketName, objectKey, HttpMethod.GET);
        signed.addRequestParameter("uploadId", uploadId);
        signed.addRequestParameter("max-parts", String.valueOf(1));
        signed.addRequestParameter("part-number-marker", String.valueOf(partNumber - 1));

        val presignedUrl = s3Client.generatePresignedUrl(signed);
        val xml = request.getForObject(presignedUrl.toURI(), String.class);
        val correctXml = xml.replaceAll("ListMultipartUploadResult", "ListPartsResult");
        log.debug("xml: {}", correctXml);

        // TODO: make this better by rewriting ListPartsResultUnmarshaller
        val data = new ByteArrayInputStream(correctXml.getBytes());
        parts = new ListPartsResultUnmarshaller().unmarshall(data).getParts();
      }
    } catch (RestClientException | AmazonClientException | URISyntaxException e) {
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
      val message = String.format("Invalid etag for part with number %s does not exist for objectId %s and uploadId %s",
          partNumber, objectId, uploadId);

      throw new NotRetryableException(new IOException(message));
    }
  }

  public void finalizeUpload(String objectId, String uploadId) {
    log.debug("finalizing upload id: {}", uploadId);
    if (stateStore.isCompleted(objectId, uploadId)) {
      try {
        val etags = stateStore.getUploadStatePartEtags(objectId, uploadId);
        val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
        val request = new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, etags);

        s3Client.completeMultipartUpload(request);

        val spec = stateStore.read(objectId, uploadId);
        byte[] content = MAPPER.writeValueAsBytes(spec);
        val data = new ByteArrayInputStream(content);
        val meta = new ObjectMetadata();
        meta.setContentLength(content.length);
        val objectMetaKey = ObjectKeys.getObjectMetaKey(dataDir, objectId);

        s3Client.putObject(bucketName, objectMetaKey, data, meta);
        stateStore.delete(objectId, uploadId);
      } catch (AmazonServiceException e) {
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

  public String getUploadId(String objectId) {
    return stateStore.getUploadId(objectId);
  }

  public ObjectMetadata getObjectMetadata(String objectId) {
    try {
      val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
      return s3Client.getObjectMetadata(bucketName, objectKey);
    } catch (AmazonServiceException e) {
      log.error("Unable to retrieve object metadata for object id: {}", objectId, e);
      throw new NotRetryableException(e);
    }
  }

  public UploadProgress getUploadStatus(String objectId, String uploadId, long fileSize) {
    val spec = stateStore.read(objectId, uploadId);
    val finished = spec.getObjectSize() == fileSize;
    if (finished) {
      stateStore.markCompletedParts(objectId, uploadId, spec.getParts());

      return new UploadProgress(objectId, uploadId, spec.getParts());
    }

    log.error("Error getting upload status for objectId {} uploadId {} fileSize {}", objectId, uploadId, fileSize);
    throw new NotRetryableException();
  }

  public void cancelUploads() {
    try {
      for (val upload : listUploads()) {
        val uploadId = upload.getUploadId();
        val objectKey = upload.getKey();
        val objectId = ObjectKeys.getObjectId(dataDir, objectKey);
        val request = new AbortMultipartUploadRequest(bucketName, objectKey, uploadId);

        s3Client.abortMultipartUpload(request);
        stateStore.delete(objectId, uploadId);
      }
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    }
  }

  public void cancelUpload(String objectId, String uploadId) {
    try {
      val objectKey = ObjectKeys.getObjectKey(dataDir, objectId);
      val request = new AbortMultipartUploadRequest(bucketName, objectKey, uploadId);

      s3Client.abortMultipartUpload(request);
      stateStore.delete(objectId, uploadId);
    } catch (AmazonServiceException e) {
      log.error("Failed to cancel upload for objectId: {}, uploadId: {}: ", objectId, uploadId, e);
      throw new RetryableException(e);
    }
  }

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
  }

  List<MultipartUpload> listUploads() {
    try {
      val request = new ListMultipartUploadsRequest(bucketName);
      val response = s3Client.listMultipartUploads(request);

      return response.getMultipartUploads();
    } catch (AmazonServiceException e) {
      log.error("Failed to list uploads: ", e);
      throw new RetryableException(e);
    }
  }

}