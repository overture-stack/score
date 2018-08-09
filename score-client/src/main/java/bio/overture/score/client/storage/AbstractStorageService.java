package bio.overture.score.client.storage;

import bio.overture.score.client.download.DownloadStateStore;
import bio.overture.score.client.exception.NotResumableException;
import bio.overture.score.client.exception.NotRetryableException;
import bio.overture.score.client.exception.RetryableException;
import bio.overture.score.core.model.DataChannel;
import bio.overture.score.core.model.Part;
import bio.overture.score.core.util.Parts;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractStorageService implements StorageService {

  private static final String ICGC_TOKEN_KEY = "X-ICGC-TOKEN";
  private final DownloadStateStore downloadStateStore;
  private final RestTemplate dataTemplate;
  private final RetryTemplate retry;

  protected abstract Optional<String> getEncryptedAccessToken();

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

              request -> {
                request.getHeaders().set(HttpHeaders.RANGE, Parts.getHttpRangeValue(part));
                String token = getEncryptedAccessToken().orElse("");
                request.getHeaders().set(ICGC_TOKEN_KEY, token);
              },

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

  @Override
  public void finalizeDownload(File outDir, String objectId) throws IOException {
    log.debug("finalizing download, object-id: {}", objectId);
    if (downloadStateStore.canFinalize(outDir, objectId)) {
      DownloadStateStore.close(outDir, objectId);
    } else {
      throw new NotRetryableException(new IOException("Fail download finalization"));
    }
  }

  @Override
  public void deleteDownloadPart(File stateDir, String objectId, Part part) {
    downloadStateStore.deletePart(stateDir, objectId, part);
  }

  @Override
  public boolean isDownloadDataRecoverable(File stateDir, String objectId, long fileSize) throws IOException {
    try {
      return (fileSize == downloadStateStore.getObjectSize(stateDir, objectId));
    } catch (Throwable e) {
      log.warn("Download is not recoverable: {}", e);
    }
    return false;
  }

}
