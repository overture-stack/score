package collaboratory.storage.object.store.service.upload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import collaboratory.storage.object.store.config.S3Config;
import collaboratory.storage.object.store.core.model.ObjectSpecification;
import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.core.util.ObjectStoreUtil;
import collaboratory.storage.object.store.exception.IdNotFoundException;
import collaboratory.storage.object.store.exception.InternalUnrecoverableError;
import collaboratory.storage.object.store.exception.NotRetryableException;
import collaboratory.storage.object.store.exception.RetryableException;
import collaboratory.storage.object.store.model.MetadataEntity;
import collaboratory.storage.object.store.service.MetadataService;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.amazonaws.services.s3.model.transform.Unmarshallers.ListPartsResultUnmarshaller;

/**
 * A service for object upload
 */
@Service
@Setter
@Slf4j
public class ObjectUploadService {

  @Autowired
  private ObjectURLGenerator urlGenerator;

  @Autowired
  private AmazonS3 s3Client;

  @Autowired
  private MetadataService metadataClient;

  @Value("${collaboratory.bucket.name}")
  private String bucketName;

  @Value("${collaboratory.data.directory}")
  private String dataDir;

  @Value("${collaboratory.upload.expiration}")
  private int expiration;

  @Autowired
  private S3Config s3Conf;

  @Autowired
  private UploadStateStore stateStore;

  @Autowired
  ObjectPartCalculator partCalculator;

  @PostConstruct
  public void init() {
  }

  public void verifyRegistration(String objectId) {
    MetadataEntity mde = metadataClient.getEntity(objectId);
    if (!mde.getId().equals(objectId)) {
      String msg = String.format(
          "Critical Error: checked for {} and Metadata Service returned {} as match",
          objectId, mde.getId());
      log.error(msg); // log to audit log file
      throw new InternalUnrecoverableError(msg);
    }
  }

  public ObjectSpecification initiateUpload(String objectId, long fileSize, boolean overwrite) {
    verifyRegistration(objectId);

    String objectKey = ObjectStoreUtil.getObjectKey(dataDir, objectId);
    log.debug("initiate upload for object key: {}, overwrite: {}", objectKey, overwrite);
    if (!overwrite) {
      if (exists(objectId)) {
        String msg = String.format("Attempted to overwrite object id {}", objectId);
        log.error(msg); // log overwrite attempt occurrence to audit log file
        throw new InternalUnrecoverableError(msg);
      }
    }

    // - check if object exists already
    try {
      String uploadId = getUploadId(objectId);
      stateStore.delete(objectId, uploadId);
    } catch (IdNotFoundException e) {
      log.info("No upload ID found. Initiate upload...");
    }

    InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(bucketName, objectKey);
    try {
      s3Conf.encrypt(req);
      InitiateMultipartUploadResult result = s3Client.initiateMultipartUpload(req);

      List<Part> parts = partCalculator.divide(fileSize);

      LocalDateTime now = LocalDateTime.now();
      Date expirationDate = Date.from(now.plusDays(expiration).atZone(ZoneId.systemDefault()).toInstant());
      for (Part part : parts) {
        part.setUrl(urlGenerator.getUploadPartUrl(bucketName, objectKey, result.getUploadId(), part, expirationDate));
      }
      ObjectSpecification spec = new ObjectSpecification(objectKey, objectId, result.getUploadId(), parts, fileSize);
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
    String objectKey = ObjectStoreUtil.getObjectKey(dataDir, objectId);
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

  private boolean isPartExist(String objectKey, String uploadId, int partNumber, String eTag) {
    List<PartSummary> parts = null;
    try {
      if (s3Conf.getEndpoint() == null) {
        ListPartsRequest req =
            new ListPartsRequest(bucketName, objectKey, uploadId);
        req.setPartNumberMarker(partNumber - 1);
        req.setMaxParts(1);
        parts = s3Client.listParts(req).getParts();
      } else {
        // HACK: Incompatible API. Serialization issue at the XML
        RestTemplate req = new RestTemplate();
        GeneratePresignedUrlRequest signedReq = new GeneratePresignedUrlRequest(bucketName, objectKey, HttpMethod.GET);
        signedReq.addRequestParameter("uploadId", uploadId);
        signedReq.addRequestParameter("max-parts", String.valueOf(1));
        signedReq.addRequestParameter("part-number-marker", String.valueOf(partNumber - 1));

        String correctXml =
            req.getForObject(s3Client.generatePresignedUrl(signedReq).toURI(), String.class).replaceAll(
                "ListMultipartUploadResult", "ListPartsResult");
        log.debug("xml: {}", correctXml);
        // TODO: make this better by rewriting ListPartsResultUnmarshaller
        parts = new ListPartsResultUnmarshaller().unmarshall(new
            ByteArrayInputStream(correctXml.getBytes())).getParts();
      }
    } catch (RestClientException | AmazonClientException | URISyntaxException e) {
      throw new RetryableException(e);
    } catch (Exception e) {
      throw new NotRetryableException(e);
    }

    if (parts != null && parts.size() != 0) {
      PartSummary part = parts.get(0);
      if (part.getPartNumber() == partNumber && part.getETag().equals(eTag)) {
        return true;
      }
    }
    return false;
  }

  @SneakyThrows
  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String eTag) {
    if (md5 != null && eTag != null && !md5.isEmpty() && !eTag.isEmpty()) {
      if (isPartExist(ObjectStoreUtil.getObjectKey(dataDir, objectId), uploadId, partNumber, eTag)) {
        stateStore.finalizeUploadPart(objectId, uploadId, partNumber, md5,
            eTag);
      } else {
        throw new NotRetryableException(new IOException("Part does not exist: " + partNumber));
      }
    } else {
      throw new NotRetryableException(new IOException("Invalid etag"));
    }
  }

  public void finalizeUpload(String objectId, String uploadId) {
    log.debug("finalizing upload id: {}", uploadId);
    if (stateStore.isCompleted(objectId, uploadId)) {
      try {
        List<PartETag> etags = stateStore.getUploadStatePartEtags(objectId, uploadId);
        s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, ObjectStoreUtil
            .getObjectKey(dataDir, objectId), uploadId, etags));
        ObjectSpecification spec = stateStore.loadUploadSpecification(objectId, uploadId);
        ObjectMapper mapper = new ObjectMapper();
        byte[] content = mapper.writeValueAsBytes(spec);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(content.length);
        s3Client.putObject(bucketName, ObjectStoreUtil.getObjectMetaKey(dataDir, objectId),
            new ByteArrayInputStream(content),
            meta);
        stateStore.delete(objectId, uploadId);
      } catch (AmazonServiceException e) {
        log.error("Service problem: {}", e);
        throw new RetryableException(e);
      } catch (IOException e) {
        log.error("Serialization problem: {}", e);
        throw new InternalUnrecoverableError();
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
      return s3Client.getObjectMetadata(bucketName, ObjectStoreUtil.getObjectKey(dataDir, objectId));
    } catch (AmazonServiceException e) {
      log.error("Unable to retrieve object metadata for object id: {}", objectId, e);
      throw new NotRetryableException(e);
    }
  }

  public UploadProgress getUploadStatus(String objectId, String uploadId, long fileSize) {
    ObjectSpecification spec = stateStore.loadUploadSpecification(objectId, uploadId);
    if (spec.getObjectSize() == fileSize) {
      stateStore.markCompletedParts(objectId, uploadId, spec.getParts());
      return new UploadProgress(objectId, uploadId, spec.getParts());
    }
    throw new NotRetryableException();
  }

  public void cancelAllUpload() {
    try {
      ListMultipartUploadsRequest req = new ListMultipartUploadsRequest(bucketName);
      MultipartUploadListing uploads = s3Client.listMultipartUploads(req);
      for (MultipartUpload upload : uploads.getMultipartUploads()) {
        AbortMultipartUploadRequest abort =
            new AbortMultipartUploadRequest(bucketName, upload.getKey(), upload.getUploadId());
        s3Client.abortMultipartUpload(abort);

      }
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    }

  }

  public void cancelUpload(String objectId, String uploadId) {
    try {
      AbortMultipartUploadRequest request =
          new AbortMultipartUploadRequest(bucketName, ObjectStoreUtil.getObjectKey(dataDir, objectId), uploadId);
      s3Client.abortMultipartUpload(request);
      stateStore.delete(objectId, uploadId);
    } catch (AmazonServiceException e) {
      throw new RetryableException(e);
    }
  }

  public void recover(String objectId, long fileSize) {
    if (fileSize != stateStore.loadUploadSpecification(objectId, getUploadId(objectId)).getObjectSize()) {
      throw new NotRetryableException();
    }
  }

  public void deletePart(String objectId, String uploadId, int partNumber) {
    stateStore.deleletePart(objectId, uploadId, partNumber);
  }
}