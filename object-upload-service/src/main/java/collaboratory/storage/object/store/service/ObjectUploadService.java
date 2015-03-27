package collaboratory.storage.object.store.service;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.Setter;
import lombok.SneakyThrows;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.store.core.util.ObjectStoreUtil;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;

@Service
@Setter
public class ObjectUploadService {

  @Autowired
  private AmazonS3 s3Client;

  @Value("${collaboratory.bucket.name}")
  private String bucketName;

  @Autowired
  private UploadStateStore stateStore;

  @Autowired
  ObjectPartCalculator partCalculator;

  @PostConstruct
  public void init() {
  }

  @SneakyThrows
  public UploadSpecification initiateUpload(String objectId, long fileSize) {

    InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(
        bucketName, objectId);
    InitiateMultipartUploadResult result = s3Client.initiateMultipartUpload(req);

    List<Part> parts = partCalculator.divide(fileSize);
    String objectKey = ObjectStoreUtil.getObjectKey(objectId);
    for (Part part : parts) {
      insertPartUploadUrl(objectKey, result.getUploadId(), part);
    }
    UploadSpecification spec = new UploadSpecification(objectKey, objectId, result.getUploadId(), parts);
    stateStore.create(spec);
    return spec;

  }

  @SneakyThrows
  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String eTag) {
    stateStore.finalizeUploadPart(objectId, uploadId, partNumber, md5, eTag);
  }

  private void insertPartUploadUrl(String objectKey, String uploadId, Part part) {
    GeneratePresignedUrlRequest req =
        new GeneratePresignedUrlRequest(bucketName, objectKey, HttpMethod.PUT);
    req.addRequestParameter("partNumber", String.valueOf(part.getPartNumber()));
    req.addRequestParameter("uploadId", uploadId);
    part.setUrl(s3Client.generatePresignedUrl(req).toString());
  }

  @SneakyThrows
  public void finalizeUpload(String objectId, String uploadId) {
    if (stateStore.isCompleted(objectId, uploadId)) {
      List<PartETag> etags = stateStore.getUploadStatePartEtags(objectId, uploadId);
      s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, ObjectStoreUtil
          .getObjectKey(objectId), uploadId, etags));
      UploadSpecification spec = stateStore.loadUploadSpecification(objectId, uploadId);
      ObjectMapper mapper = new ObjectMapper();
      s3Client.putObject(bucketName, spec.getObjectKey(), new ByteArrayInputStream(mapper.writeValueAsBytes(spec)),
          null);
      stateStore.delete(objectId, uploadId);
    }
  }

  @SneakyThrows
  public UploadSpecification getIncompletedUploadParts(String objectId, String uploadId) {
    return new UploadSpecification(ObjectStoreUtil.getObjectKey(objectId), objectId, uploadId,
        stateStore.retrieveIncompletedPart(objectId, uploadId));
  }

  public void cancelUpload(String objectId, String uploadId) {
    AbortMultipartUploadRequest request =
        new AbortMultipartUploadRequest(bucketName, ObjectStoreUtil.getObjectKey(objectId), uploadId);
    s3Client.abortMultipartUpload(request);
    stateStore.delete(objectId, uploadId);
  }
}