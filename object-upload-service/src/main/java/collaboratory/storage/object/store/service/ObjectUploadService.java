package collaboratory.storage.object.store.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.store.core.util.ObjectStoreUtil;

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

@Service
@Setter
@Slf4j
public class ObjectUploadService {

  @Autowired
  private AmazonS3 s3Client;

  @Value("${collaboratory.bucket.name}")
  private String bucketName;

  @Value("${collaboratory.data.directory}")
  private String dataDir;

  @Value("${collaboratory.upload.expiration}")
  private int expiration;

  @Value("${s3.endpoint}")
  private String endPoint;

  @Autowired
  private UploadStateStore stateStore;

  @Autowired
  ObjectPartCalculator partCalculator;

  @PostConstruct
  public void init() {
  }

  @SneakyThrows
  public UploadSpecification initiateUpload(String objectId, long fileSize) {
    // TODO: check if a upload for the object id is already initiated, if no start, else delete it first before initiate
    // TODO: check if the object already exists
    String objectKey = ObjectStoreUtil.getObjectKey(dataDir, objectId);
    InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(
        bucketName, objectKey);
    InitiateMultipartUploadResult result = s3Client.initiateMultipartUpload(req);

    List<Part> parts = partCalculator.divide(fileSize);

    LocalDateTime now = LocalDateTime.now();
    Date expirationDate = Date.from(now.plusDays(expiration).atZone(ZoneId.systemDefault()).toInstant());
    for (Part part : parts) {
      insertPartUploadUrl(objectKey, result.getUploadId(), part, expirationDate);
    }
    UploadSpecification spec = new UploadSpecification(objectKey, objectId, result.getUploadId(), parts);
    stateStore.create(spec);
    return spec;
  }

  // TODO: Ceph does not provide ways to query parts info
  @SneakyThrows
  private boolean isPartExist(String objectKey, String uploadId, int partNumber, String eTag) {
    List<PartSummary> parts = null;
    if (endPoint == null) {
      ListPartsRequest req =
          new ListPartsRequest(bucketName, objectKey, uploadId);
      req.setPartNumberMarker(partNumber - 1);
      req.setMaxParts(1);
      parts = s3Client.listParts(req).getParts();
    } else {
      // HACK: Incompatible API
      RestTemplate req = new RestTemplate();
      GeneratePresignedUrlRequest signedReq = new GeneratePresignedUrlRequest(bucketName, objectKey, HttpMethod.GET);
      signedReq.addRequestParameter("uploadId", uploadId);
      signedReq.addRequestParameter("max-parts", String.valueOf(1));
      signedReq.addRequestParameter("part-number-marker", String.valueOf(partNumber - 1));

      String xml = req.getForObject(s3Client.generatePresignedUrl(signedReq).toURI(), String.class);
      String correctXml =
          xml.replaceAll("ListMultipartUploadResult", "ListPartsResult");
      log.debug("xml: {}", correctXml);
      parts = new ListPartsResultUnmarshaller().unmarshall(new
          ByteArrayInputStream(correctXml.getBytes())).getParts();
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
      // TODO: re-enable after apply ceph fix: http://tracker.ceph.com/issues/10271
      if (isPartExist(ObjectStoreUtil.getObjectKey(dataDir, objectId), uploadId, partNumber, eTag)) {
        stateStore.finalizeUploadPart(objectId, uploadId, partNumber, md5,
            eTag);
      } else {
        throw new IOException("Part does not exist: " + partNumber);
      }
    } else {
      throw new IOException("Invalid etag");
    }
  }

  private void insertPartUploadUrl(String objectKey, String uploadId, Part part, Date expiration) {
    GeneratePresignedUrlRequest req =
        new GeneratePresignedUrlRequest(bucketName, objectKey, HttpMethod.PUT);
    req.setExpiration(expiration);
    req.addRequestParameter("partNumber", String.valueOf(part.getPartNumber()));
    req.addRequestParameter("uploadId", uploadId);
    part.setUrl(s3Client.generatePresignedUrl(req).toString());
  }

  @SneakyThrows
  public void finalizeUpload(String objectId, String uploadId) {
    if (stateStore.isCompleted(objectId, uploadId)) {
      List<PartETag> etags = stateStore.getUploadStatePartEtags(objectId, uploadId);
      s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, ObjectStoreUtil
          .getObjectKey(dataDir, objectId), uploadId, etags));
      UploadSpecification spec = stateStore.loadUploadSpecification(objectId, uploadId);
      ObjectMapper mapper = new ObjectMapper();
      byte[] content = mapper.writeValueAsBytes(spec);
      ObjectMetadata meta = new ObjectMetadata();
      meta.setContentLength(content.length);
      s3Client.putObject(bucketName, ObjectStoreUtil.getObjectMetaKey(dataDir, objectId),
          new ByteArrayInputStream(content),
          meta);
      stateStore.delete(objectId, uploadId);
    } else {
      throw new IOException("Not found");
    }
  }

  public String getUploadId(String objectId) {
    return stateStore.getUploadId(objectId);
  }

  @SneakyThrows
  public UploadProgress getUploadProgress(String objectId, String uploadId) {
    UploadSpecification spec = stateStore.loadUploadSpecification(objectId, uploadId);
    return new UploadProgress(objectId, uploadId, spec.getParts(),
        stateStore.retrieveCompletedParts(objectId, uploadId));
  }

  public void cancelAllUpload() {
    ListMultipartUploadsRequest req = new ListMultipartUploadsRequest(bucketName);
    MultipartUploadListing uploads = s3Client.listMultipartUploads(req);
    for (MultipartUpload upload : uploads.getMultipartUploads()) {
      AbortMultipartUploadRequest abort =
          new AbortMultipartUploadRequest(bucketName, upload.getKey(), upload.getUploadId());
      s3Client.abortMultipartUpload(abort);

    }

  }

  public void cancelUpload(String objectId, String uploadId) {
    AbortMultipartUploadRequest request =
        new AbortMultipartUploadRequest(bucketName, ObjectStoreUtil.getObjectKey(dataDir, objectId), uploadId);
    s3Client.abortMultipartUpload(request);
    stateStore.delete(objectId, uploadId);
  }
}