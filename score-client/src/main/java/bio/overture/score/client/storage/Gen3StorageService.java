package bio.overture.score.client.storage;

import bio.overture.score.client.download.DownloadStateStore;
import bio.overture.score.core.model.DataChannel;
import bio.overture.score.core.model.ObjectInfo;
import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.core.model.Part;
import bio.overture.score.core.model.UploadProgress;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@Profile("gen3")
public class Gen3StorageService extends AbstractStorageService{

  private static final  String NOT_IMPLEMENTED_MESSAGE = "Not implemented for Gen3 Storage";

  /**
   * Configuration.
   */
  private String endpoint;

  /**
   * Dependencies.
   */
  private DownloadStateStore downloadStateStore;
  private RestTemplate dataTemplate;
  private RetryTemplate retry;

  @Autowired
  public Gen3StorageService(
      @Value("${storage.url}") @NonNull String endpoint,
      @Autowired @NonNull DownloadStateStore downloadStateStore,
      @Qualifier("pingTemplate") @NonNull RestTemplate dataTemplate,
      @NonNull RetryTemplate retry) {
    super(endpoint, downloadStateStore, dataTemplate, retry);
    this.downloadStateStore = downloadStateStore;
    this.dataTemplate = dataTemplate;
    this.retry = retry;
    this.endpoint = endpoint;
    log.info("*****************LOADED GEN3 STORAGE SERVICE");
  }

  @Override protected Optional<String> getEncryptedAccessToken() {
    return Optional.empty();
  }

  @Override public ObjectSpecification getDownloadSpecification(String objectId, long offset, long length)
      throws IOException {
    return null;
  }

  @Override public ObjectSpecification getExternalDownloadSpecification(String objectId, long offset, long length)
      throws IOException {
    return null;
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
