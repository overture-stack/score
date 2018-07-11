package bio.overture.score.client.transport;

import bio.overture.score.client.config.ClientProperties;
import bio.overture.score.client.download.DownloadStateStore;
import bio.overture.score.client.encryption.TokenEncryptionService;
import bio.overture.score.client.exception.NotResumableException;
import bio.overture.score.client.exception.NotRetryableException;
import bio.overture.score.client.exception.RetryableException;
import bio.overture.score.core.model.DataChannel;
import bio.overture.score.core.model.ObjectInfo;
import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.core.model.Part;
import bio.overture.score.core.model.UploadProgress;
import bio.overture.score.core.util.Parts;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
@Service
public class Gen3StorageService implements StorageServiceInterface {

  /**
   * Configuration.
   */
  @Value("${storage.url}")
  private String endpoint;

  /**
   * Dependencies.
   */
  @Autowired
  private DownloadStateStore downloadStateStore;
  @Autowired
  @Qualifier("serviceTemplate")
  private RestTemplate serviceTemplate;
  @Autowired
  @Qualifier("dataTemplate")
  private RestTemplate dataTemplate;
  @Autowired
  @Qualifier("pingTemplate")
  private RestTemplate pingTemplate;
  @Autowired
  private RetryTemplate retry;
  @Autowired
  @Qualifier("clientVersion")
  private String clientVersion;
  @Autowired
  private ClientProperties properties;
  @Autowired
  private TokenEncryptionService tokenEncryptionService;

  @Override public void downloadPart(DataChannel channel, Part part, String objectId, File outputDir)
      throws IOException {
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        log.debug("Download Part URL: {}", part.getUrl());

        try {
          // the actual GET operation
          log.debug("performing GET {}", part.getUrl());
          String md5 = dataTemplate.execute(new URI(part.getUrl()), HttpMethod.GET,
              request ->
                request.getHeaders()
                    .set(HttpHeaders.RANGE, Parts.getHttpRangeValue(part)),
              response -> {
                try (HashingInputStream his = new HashingInputStream(Hashing.md5(), response.getBody())) {
                  channel.readFrom(his);
                  return his.hash().toString();
                }
              });

          part.setMd5(md5);
          checkState(!part.hasFailedChecksum(), "Checksum failed for Part# %s: %s", part.getPartNumber(),
              part.getMd5());

          // TODO: try catch here for commit
          downloadStateStore.commit(outputDir, objectId, part);
          log.debug("committed {} part# {} to download state store", objectId, part.getPartNumber());
        } catch (NotResumableException | NotRetryableException e) {
          log.error("Cannot proceed. Failed to receive part for part# {} : {}", part.getPartNumber(), e.getMessage());
          throw e;
        } catch (Throwable e) {
          log.warn("Failed to receive part for part number: {}. Retrying. {}", part.getPartNumber(), e.getMessage());
          channel.reset();
          throw new RetryableException(e);
        }
        return null;
      }
    });

  }

  @Override public void finalizeDownload(File outDir, String objectId) throws IOException {

  }

  @Override public ObjectSpecification getDownloadSpecification(String objectId) throws IOException {
    return null;
  }

  @Override public ObjectSpecification getDownloadSpecification(String objectId, long offset, long length)
      throws IOException {
    return null;
  }

  @Override public ObjectSpecification getExternalDownloadSpecification(String objectId, long offset, long length)
      throws IOException {
    return null;
  }

  @Override public void deleteDownloadPart(File stateDir, String objectId, Part part) {

  }

  @Override public boolean isDownloadDataRecoverable(File stateDir, String objectId, long fileSize) throws IOException {
    return false;
  }

  @Override public String ping() {
    return null;
  }

  /**
   *  Not implemented for Gen3
   */
  @Override public boolean isUploadDataRecoverable(String objectId, long fileSize) throws IOException {
    throw new IllegalStateException("Not implemented for Gen3 Storage");
  }

  @Override public void deleteUploadPart(String objectId, String uploadId, Part part) throws IOException {
    throw new IllegalStateException("Not implemented for Gen3 Storage");
  }

  @Override public void finalizeUpload(String objectId, String uploadId) throws IOException {
    throw new IllegalStateException("Not implemented for Gen3 Storage");
  }

  @Override public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String etag,
      boolean disableChecksum) throws IOException {
    throw new IllegalStateException("Not implemented for Gen3 Storage");
  }

  @Override public boolean isObjectExist(String objectId) throws IOException {
    throw new IllegalStateException("Not implemented for Gen3 Storage");
  }

  @Override public void uploadPart(DataChannel channel, Part part, String objectId, String uploadId)
      throws IOException {
    throw new IllegalStateException("Not implemented for Gen3 Storage");
  }

  @Override public List<ObjectInfo> listObjects() {
    throw new IllegalStateException("Not implemented for Gen3 Storage");
  }

  @Override public UploadProgress getProgress(String objectId, long fileSize) throws IOException {
    throw new IllegalStateException("Not implemented for Gen3 Storage");
  }

  @Override public ObjectSpecification initiateUpload(String objectId, long length, boolean overwrite, String md5)
      throws IOException {
    throw new IllegalStateException("Not implemented for Gen3 Storage");
  }

}
