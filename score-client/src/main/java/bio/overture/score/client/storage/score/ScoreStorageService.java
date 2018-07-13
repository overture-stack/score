/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package bio.overture.score.client.storage.score;

import bio.overture.score.client.config.ClientProperties;
import bio.overture.score.client.download.DownloadStateStore;
import bio.overture.score.client.encryption.TokenEncryptionService;
import bio.overture.score.client.exception.NotResumableException;
import bio.overture.score.client.exception.NotRetryableException;
import bio.overture.score.client.exception.RetryableException;
import bio.overture.score.client.storage.AbstractStorageService;
import bio.overture.score.core.model.DataChannel;
import bio.overture.score.core.model.ObjectInfo;
import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.core.model.Part;
import bio.overture.score.core.model.UploadProgress;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

/**
 * Service responsible for interacting with the remote upload service.
 */
@Slf4j
@Service
@Profile({"dev", "collab", "aws", "default", "!gen3"})
public class ScoreStorageService extends AbstractStorageService {

  /**
   * Configuration.
   */
  private String endpoint;

  /**
   * Dependencies.
   */
  private RestTemplate serviceTemplate;
  private RestTemplate pingTemplate;
  private String clientVersion;
  private ClientProperties properties;
  private TokenEncryptionService tokenEncryptionService;
  private RetryTemplate retry;
  private RestTemplate dataTemplate;

  @Autowired
  public ScoreStorageService(
      @Value("${storage.url}") @NonNull String endpoint,
      @NonNull DownloadStateStore downloadStateStore,
      @Qualifier("dataTemplate") @NonNull RestTemplate dataTemplate,
      @NonNull RetryTemplate retry,
      @Qualifier("serviceTemplate") @NonNull RestTemplate serviceTemplate,
      @Qualifier("pingTemplate") @NonNull RestTemplate pingTemplate,
      @Qualifier("clientVersion") @NonNull String clientVersion,
      @NonNull ClientProperties properties,
      @NonNull TokenEncryptionService tokenEncryptionService ) {
    super(downloadStateStore, dataTemplate, retry);
    this.dataTemplate = dataTemplate;
    this.retry = retry;
    this.serviceTemplate = serviceTemplate;
    this.pingTemplate = pingTemplate;
    this.clientVersion = clientVersion;
    this.properties = properties;
    this.tokenEncryptionService = tokenEncryptionService;
    this.endpoint = endpoint;
    log.info("**********************LOADED SCORE STORAGE SERVICE");
  }

  @Override @SneakyThrows
  public List<ObjectInfo> listObjects() {
    log.debug("Listing objects...");
    return retry.execute(
        ctx -> serviceTemplate.exchange(endpoint + "/listing", GET,
            defaultEntity(),
            new ParameterizedTypeReference<List<ObjectInfo>>() {}).getBody());
  }

  @Override public UploadProgress getProgress(String objectId, long fileSize) throws IOException {
    return retry.execute(
        ctx -> serviceTemplate.exchange(endpoint + "/upload/{object-id}/status?fileSize={file-size}", GET,
            defaultEntity(),
            UploadProgress.class, objectId, fileSize).getBody());
  }

  protected String cleanUpETag(String eTag) {
    return eTag.replaceAll("^\"|\"$", "");
  }

  @Override public void uploadPart(DataChannel channel, Part part, String objectId, String uploadId) throws IOException {
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        log.debug("Upload Part URL: {}", part.getUrl());

        final RequestCallback callback = request -> {
          HttpHeaders requestHeader = request.getHeaders();
          requestHeader.setContentLength(channel.getLength());
          try (OutputStream os = request.getBody()) {
            channel.writeTo(os);
          }
        };

        final ResponseExtractor<HttpHeaders> headersExtractor = response -> response.getHeaders();

        try {
          HttpHeaders headers =
              dataTemplate.execute(new URI(part.getUrl()), HttpMethod.PUT, callback, headersExtractor);

          try {
            finalizeUploadPart(objectId, uploadId, part.getPartNumber(), channel.getMd5(),
                cleanUpETag(headers.getETag()), disableChecksum(headers));
          } catch (NotRetryableException e) {
            log.warn("Checksum failed for part #{}, MD5={}, ETAG={} : {}", part, channel.getMd5(), headers.getETag(),
                e);
            throw new RetryableException(e);
          }
        } catch (NotResumableException | NotRetryableException e) {
          log.error("Could not proceed. Failed to send part for part number: {}", part.getPartNumber(), e);
          throw e;
        } catch (Throwable e) {
          log.warn("Failed to send part for part #{} : {}", part.getPartNumber(), e);
          channel.reset();
          throw new RetryableException(e);
        }
        return null;
      }

      private boolean disableChecksum(HttpHeaders headers) {
        List<String> encryption = headers.get(Headers.SERVER_SIDE_ENCRYPTION);
        if (encryption != null && !encryption.isEmpty()) {
          return encryption.contains(SSEAlgorithm.KMS.getAlgorithm());
        }
        return false;
      }
    });
  }

  @Override public ObjectSpecification initiateUpload(String objectId, long length, boolean overwrite, String md5)
      throws IOException {
    log.debug("Initiating upload, object-id: {} overwrite: {}", objectId, overwrite);
    return retry.execute(ctx -> serviceTemplate.exchange(
        endpoint + "/upload/{object-id}/uploads?fileSize={file-size}&overwrite={overwrite}&md5={checksum}",
        POST,
        defaultEntity(),
        ObjectSpecification.class, objectId, length, overwrite, md5).getBody());
  }

  @Override public void finalizeUpload(String objectId, String uploadId) throws IOException {
    log.debug("finalizing upload, object-id: {}, upload-id: {}", objectId, uploadId);
    retry.execute(ctx -> {
      serviceTemplate.exchange(endpoint + "/upload/{object-id}?uploadId={upload-id}", HttpMethod.POST, defaultEntity(),
          Void.class, objectId, uploadId);
      return null;
    });
    log.debug("finalizing upload returned");
  }

  @Override public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String etag,
      boolean disableChecksum)
      throws IOException {
    log.debug("Finalizing upload part, object-id: {}, upload-id: {}, part-number: {}", objectId, uploadId, partNumber);
    retry.execute(ctx -> {
      if (disableChecksum || md5.equals(etag)) {
        serviceTemplate
            .exchange(
                endpoint
                    + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}&md5={md5}&etag={etag}",
                HttpMethod.POST, defaultEntity(),
                Void.class, objectId, uploadId, partNumber, md5, etag);
        return null;
      }
      throw new NotRetryableException(); // using this as control mechanism?
    });
  }

  @Override public boolean isObjectExist(String objectId) throws IOException {
    log.debug("Checking existence on Storage server for object-id: {}", objectId);
    return retry.execute(ctx -> {
      boolean result = serviceTemplate.exchange(endpoint + "/upload/{object-id}",
          GET, defaultEntity(),
          Boolean.class, objectId).getBody();
      return result;
    });

  }

  @Override public ObjectSpecification getDownloadSpecification(String objectId, long offset, long length) throws IOException {
    log.debug("Endpoint: {}", endpoint);
    return retry.execute(ctx -> {
      return serviceTemplate.exchange(endpoint + "/download/{object-id}?offset={offset}&length={length}",
          GET,
          defaultEntity(),
          ObjectSpecification.class, objectId, offset, length).getBody();
    });
  }

  /**
   * Returns ObjectSpecification containing only a single part in order to generate a single pre-signed URL that
   * external clients can use (i.e., curl - something that doesn't understand our parts). The external query parameter
   * is set to true.
   */
  @Override
  public ObjectSpecification getExternalDownloadSpecification(String objectId, long offset, long length)
      throws IOException {
    log.debug("Endpoint: {}", endpoint);
    return retry.execute(ctx -> serviceTemplate.exchange(
        endpoint + "/download/{object-id}?offset={offset}&length={length}&external=true",
        GET,
        defaultEntity(),
        ObjectSpecification.class, objectId, offset, length).getBody());
  }


  @Override public void deleteUploadPart(String objectId, String uploadId, Part part) throws IOException {
    log.debug("Deleting part for object-id: {}, upload-id: {}, part: {}", objectId, uploadId, part);
    retry.execute(ctx -> {
      serviceTemplate.exchange(
          endpoint + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}",
          DELETE, defaultEntity(),
          Void.class, objectId, uploadId, part.getPartNumber());
      return null;
    });
  }


  @Override public boolean isUploadDataRecoverable(String objectId, long fileSize) throws IOException {
    log.debug("Recovering upload, object-id: {}", objectId);
    return retry.execute(ctx -> {
      try {
        serviceTemplate.exchange(
            endpoint + "/upload/{object-id}/recovery?fileSize={file-size}",
            HttpMethod.POST, defaultEntity(),
            Boolean.class, objectId, fileSize);
      } catch (NotRetryableException e) {
        return false;
      }
      return true;
    });
  }

  @Override public String ping() {
    // Get pre-signed URL to retrieve sentinel object from bucket
    try {
      val signedUrl =
          serviceTemplate.exchange(endpoint + "/download/ping", HttpMethod.GET, defaultEntity(), String.class)
              .getBody();
      URI uri = null;
      try {
        uri = new URI(signedUrl);
      } catch (URISyntaxException use) {
        // This should never happen since the URI is generated using the S3 Java SDK
        log.error(use.getMessage());
        throw use;
      }
      String result = pingTemplate.getForObject(uri, String.class);
      return result;
    } catch (RestClientException rce) {
      if (rce.getRootCause().getClass().equals(SocketTimeoutException.class)) {
        log.error(
            "Unable to connect to repository endpoint. Verify your network connection. You also need to be running on a compute node within the repository cloud.");
        throw new NotRetryableException(new IOException(
            "Access refused by repository. Ensure client is running as part of repository cloud."));
      }
      // Some other unanticipated error
      throw new NotRetryableException(rce);
    } catch (NotRetryableException nre) {
      log.error(nre.getMessage());
      throw nre;
    } catch (Exception e) {
      log.error("Received unexpected exception: " + e.getMessage());
      throw new NotRetryableException(e);
    }
  }

  private HttpEntity<Object> defaultEntity() {
    return new HttpEntity<Object>(defaultHeaders());
  }

  private HttpHeaders defaultHeaders() {
    val requestHeaders = new HttpHeaders();
    requestHeaders.add(HttpHeaders.USER_AGENT, clientVersion);
    return requestHeaders;
  }

  @Override
  protected Optional<String> getEncryptedAccessToken() {
    val encryptedToken = tokenEncryptionService.encryptAccessToken(properties.getAccessToken());
    val tokenValue = encryptedToken.isPresent() ? encryptedToken.get() : "";
    properties.setEncryptedAccessToken(tokenValue);
    return encryptedToken;
  }

}
