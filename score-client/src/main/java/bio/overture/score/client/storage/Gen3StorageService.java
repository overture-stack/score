package bio.overture.score.client.storage;

import bio.overture.score.client.download.DownloadStateStore;
import bio.overture.score.client.storage.Gen3Client.PresignedUrl;
import bio.overture.score.core.model.DataChannel;
import bio.overture.score.core.model.ObjectInfo;
import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.core.model.Part;
import bio.overture.score.core.model.UploadProgress;
import bio.overture.score.core.util.SimplePartCalculator;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Service
@Profile("gen3")
public class Gen3StorageService extends AbstractStorageService{

  private static final  String NOT_IMPLEMENTED_MESSAGE = "Not implemented for Gen3 Storage";

  /**
   * Dependencies.
   */
  private final Gen3Client gen3Client;
  private final SimplePartCalculator partCalculator;


  @Autowired
  public Gen3StorageService(
      @Value("${storage.url}") @NonNull String endpoint,
      @NonNull DownloadStateStore downloadStateStore,
      @Qualifier("pingTemplate") @NonNull RestTemplate dataTemplate,
      @NonNull RetryTemplate retry,
      SimplePartCalculator partCalculator,
      @NonNull Gen3Client gen3Client
  ) {
    super(endpoint, downloadStateStore, dataTemplate, retry);
    this.gen3Client = gen3Client;
    this.partCalculator = partCalculator;
    log.info("*****************LOADED GEN3 STORAGE SERVICE");
  }


  @Override
  public ObjectSpecification getDownloadSpecification(String objectId, long offset, long length)
      throws IOException {
    val presignedUrl = gen3Client.generatePresignedUrl(objectId);
    val parts = generateParts(presignedUrl, offset, length);
    return ObjectSpecification.builder()
        .objectId(objectId)
        .objectMd5(null) // useless, since we do not have the md5 values of the initial part uploads
        .objectSize(presignedUrl.getSize())
        .parts(parts)
        .objectKey(null)
        .relocated(false)
        .build();
  }

  @Override
  public ObjectSpecification getExternalDownloadSpecification(String objectId, long offset, long length)
      throws IOException {
    return getDownloadSpecification(objectId);
  }


  @Override protected Optional<String> getEncryptedAccessToken() {
    return Optional.empty();
  }

  @SneakyThrows
  private List<Part> generateParts(PresignedUrl presignedUrl, long offset, long length){
    val url = presignedUrl.getUrl();
    val totalSize = presignedUrl.getSize();
    checkArgument(offset + length < totalSize,
        "The sum of: offset(%s) + length(%s) = %s must be less than the total size (%s)",
        offset, length, offset+length, totalSize );
    val parts = partCalculator.divide(offset, length);
    parts.forEach(x -> {
      x.setMd5(null); // Unobtainable
      x.setSourceMd5(null); // useless, since we do not have the md5 values of the initial part uploads
      x.setUrl(url);
    } );
    return ImmutableList.copyOf(parts);
  }

  /**
   *  Not implemented for Gen3
   */
  @Override public String ping() {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override public boolean isUploadDataRecoverable(String objectId, long fileSize) throws IOException {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override public void deleteUploadPart(String objectId, String uploadId, Part part) throws IOException {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override public void finalizeUpload(String objectId, String uploadId) throws IOException {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String etag,
      boolean disableChecksum) throws IOException {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override public boolean isObjectExist(String objectId) throws IOException {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override public void uploadPart(DataChannel channel, Part part, String objectId, String uploadId)
      throws IOException {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override public List<ObjectInfo> listObjects() {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override public UploadProgress getProgress(String objectId, long fileSize) throws IOException {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

  @Override public ObjectSpecification initiateUpload(String objectId, long length, boolean overwrite, String md5)
      throws IOException {
    throw new IllegalStateException(NOT_IMPLEMENTED_MESSAGE);
  }

}
