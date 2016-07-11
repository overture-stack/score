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
package org.icgc.dcc.storage.client.transport;

import static com.google.common.base.Preconditions.checkState;
import static org.springframework.http.HttpMethod.GET;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.client.download.DownloadStateStore;
import org.icgc.dcc.storage.client.exception.NotResumableException;
import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.exception.RetryableException;
import org.icgc.dcc.storage.core.model.DataChannel;
import org.icgc.dcc.storage.core.model.ObjectInfo;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.core.model.UploadProgress;
import org.icgc.dcc.storage.core.util.Parts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;

/**
 * Service responsible for interacting with the remote upload service.
 */
@Slf4j
@Service
public class StorageService {

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

  @SneakyThrows
  public List<ObjectInfo> listObjects() {
    log.debug("Listing objects...");
    return retry.execute(new RetryCallback<List<ObjectInfo>, IOException>() {

      @Override
      public List<ObjectInfo> doWithRetry(RetryContext ctx) throws IOException {
        val requestEntity = defaultRequestEntity();
        return serviceTemplate.exchange(endpoint + "/listing", GET, requestEntity,
            new ParameterizedTypeReference<List<ObjectInfo>>() {}).getBody();
      }

    });
  }

  public UploadProgress getProgress(String objectId, long fileSize) throws IOException {
    return retry.execute(new RetryCallback<UploadProgress, IOException>() {

      @Override
      public UploadProgress doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        return serviceTemplate.exchange(endpoint + "/upload/{object-id}/status?fileSize={file-size}", HttpMethod.GET,
            requestEntity,
            UploadProgress.class, objectId, fileSize).getBody();
      }
    });

  }

  public void downloadPart(DataChannel channel, Part part, String objectId, File outputDir) throws IOException {
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        log.debug("Download Part URL: {}", part.getUrl());

        try {
          // the actual GET operation
          log.debug("performing GET {}", part.getUrl());
          String md5 = dataTemplate.execute(new URI(part.getUrl()), HttpMethod.GET,

              request -> request.getHeaders().set(HttpHeaders.RANGE, Parts.getHttpRangeValue(part)),

              response -> {
                try (HashingInputStream his = new HashingInputStream(Hashing.md5(), response.getBody())) {
                  channel.readFrom(his);
                  return his.hash().toString();
                }
              }
              );

          part.setMd5(md5);
          checkState(!part.hasFailedChecksum(), "Checksum failed for Part# %d: %s", part.getPartNumber(), part.getMd5());

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

  protected String cleanUpETag(String eTag) {
    return eTag.replaceAll("^\"|\"$", "");
  }

  public void uploadPart(DataChannel channel, Part part, String objectId, String uploadId) throws IOException {
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        log.debug("Upload Part URL: {}", part.getUrl());

        final RequestCallback callback = new RequestCallback() {

          @Override
          public void doWithRequest(final ClientHttpRequest request) throws IOException {
            HttpHeaders requestHeader = request.getHeaders();
            requestHeader.setContentLength(channel.getLength());
            try (OutputStream os = request.getBody()) {
              channel.writeTo(os);
            }
          }
        };

        final ResponseExtractor<HttpHeaders> headersExtractor = new ResponseExtractor<HttpHeaders>() {

          @Override
          public HttpHeaders extractData(ClientHttpResponse response) throws IOException {
            return response.getHeaders();
          }
        };

        try {
          HttpHeaders headers =
              dataTemplate.execute(new URI(part.getUrl()), HttpMethod.PUT, callback, headersExtractor);

          try {
            finalizeUploadPart(objectId, uploadId, part.getPartNumber(), channel.getMd5(),
                cleanUpETag(headers.getETag()), disableChecksum(headers));
          } catch (NotRetryableException e) {
            log.warn("Checksum failed for part #{}, MD5={}, ETAG={} : {}", part, channel.getMd5(), headers.getETag(), e);
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

  public ObjectSpecification initiateUpload(String objectId, long length, boolean overwrite, String md5)
      throws IOException {
    log.debug("Initiating upload, object-id: {} overwrite: {}", objectId, overwrite);
    return retry.execute(new RetryCallback<ObjectSpecification, IOException>() {

      @Override
      public ObjectSpecification doWithRetry(RetryContext ctx) throws IOException {
        val requestEntity = new HttpEntity<Object>(defaultHeaders());
        return serviceTemplate.exchange(
            endpoint + "/upload/{object-id}/uploads?fileSize={file-size}&overwrite={overwrite}&md5={checksum}",
            HttpMethod.POST,
            requestEntity,
            ObjectSpecification.class, objectId, length, overwrite, md5).getBody();
      }
    });
  }

  public void finalizeDownload(File outDir, String objectId) throws IOException {
    log.debug("finalizing download, object-id: {}", objectId);
    if (downloadStateStore.canFinalize(outDir, objectId)) {
      DownloadStateStore.close(outDir, objectId);
    } else {
      throw new NotRetryableException(new IOException("Fail download finalization"));
    }
  }

  public void finalizeUpload(String objectId, String uploadId) throws IOException {
    log.debug("finalizing upload, object-id: {}, upload-id: {}", objectId, uploadId);
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        val requestEntity = new HttpEntity<Object>(defaultHeaders());
        serviceTemplate.exchange(endpoint + "/upload/{object-id}?uploadId={upload-id}", HttpMethod.POST, requestEntity,
            Void.class, objectId, uploadId);
        return null;
      }
    });
    log.debug("finalizing upload returned");
  }

  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String etag,
      boolean disableChecksum)
      throws IOException {
    log.debug("Finalizing upload part, object-id: {}, upload-id: {}, part-number: {}", objectId, uploadId, partNumber);
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        if (disableChecksum || md5.equals(etag)) {
          val requestEntity = new HttpEntity<Object>(defaultHeaders());
          serviceTemplate
              .exchange(
                  endpoint
                      + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}&md5={md5}&etag={etag}",
                  HttpMethod.POST, requestEntity,
                  Void.class, objectId, uploadId, partNumber, md5, etag);
          return null;
        }
        throw new NotRetryableException(); // using this as control mechanism?
      }
    });
  }

  public boolean isObjectExist(String objectId) throws IOException {
    log.debug("Checking existence on Storage server for object-id: {}", objectId);
    return retry.execute(new RetryCallback<Boolean, IOException>() {

      @Override
      public Boolean doWithRetry(RetryContext ctx) throws IOException {
        val requestEntity = new HttpEntity<Object>(defaultHeaders());

        boolean result = serviceTemplate.exchange(endpoint + "/upload/{object-id}",
            HttpMethod.GET, requestEntity,
            Boolean.class, objectId).getBody();
        return result;
      }
    });

  }

  public ObjectSpecification getDownloadSpecification(String objectId) throws IOException {
    return getDownloadSpecification(objectId, 0, -1L);
  }

  public ObjectSpecification getDownloadSpecification(String objectId, long offset, long length) throws IOException {
    log.debug("Endpoint: {}", endpoint);
    return retry.execute(new RetryCallback<ObjectSpecification, IOException>() {

      @Override
      public ObjectSpecification doWithRetry(RetryContext ctx) throws IOException {
        val requestEntity = new HttpEntity<Object>(defaultHeaders());
        return serviceTemplate.exchange(endpoint + "/download/{object-id}?offset={offset}&length={length}",
            HttpMethod.GET,
            requestEntity,
            ObjectSpecification.class, objectId, offset, length).getBody();
      }
    });
  }

  /**
   * Returns ObjectSpecification containing only a single part in order to generate a single pre-signed URL that
   * external clients can use (i.e., curl - something that doesn't understand our parts). The external query parameter
   * is set to true.
   */
  public ObjectSpecification getExternalDownloadSpecification(String objectId, long offset, long length)
      throws IOException {
    log.debug("Endpoint: {}", endpoint);
    return retry.execute(new RetryCallback<ObjectSpecification, IOException>() {

      @Override
      public ObjectSpecification doWithRetry(RetryContext ctx) throws IOException {
        val requestEntity = new HttpEntity<Object>(defaultHeaders());
        return serviceTemplate.exchange(
            endpoint + "/download/{object-id}?offset={offset}&length={length}&external=true",
            HttpMethod.GET,
            requestEntity,
            ObjectSpecification.class, objectId, offset, length).getBody();
      }
    });
  }

  public void deleteDownloadPart(File stateDir, String objectId, Part part) {
    downloadStateStore.deletePart(stateDir, objectId, part);

  }

  public void deleteUploadPart(String objectId, String uploadId, Part part) throws IOException {
    log.debug("Deleting part for object-id: {}, upload-id: {}, part: {}", objectId, uploadId, part);
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        val requestEntity = new HttpEntity<Object>(defaultHeaders());
        serviceTemplate.exchange(
            endpoint + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}",
            HttpMethod.DELETE, requestEntity,
            Void.class, objectId, uploadId, part.getPartNumber());
        return null;
      }
    });
  }

  public boolean isDownloadDataRecoverable(File stateDir, String objectId, long fileSize) throws IOException {
    try {
      return (fileSize == downloadStateStore.getObjectSize(stateDir, objectId));
    } catch (Throwable e) {
      log.warn("Download is not recoverable: {}", e);
    }
    return false;

  }

  public boolean isUploadDataRecoverable(String objectId, long fileSize) throws IOException {
    log.debug("Recovering upload, object-id: {}", objectId);
    return retry.execute(new RetryCallback<Boolean, IOException>() {

      @Override
      public Boolean doWithRetry(RetryContext ctx) throws IOException {
        try {
          val requestEntity = new HttpEntity<Object>(defaultHeaders());
          serviceTemplate.exchange(
              endpoint + "/upload/{object-id}/recovery?fileSize={file-size}",
              HttpMethod.POST, requestEntity,
              Boolean.class, objectId, fileSize);
        } catch (NotRetryableException e) {
          return false;
        }
        return true;
      }
    });
  }

  public String ping() {
    HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
    // get pre-signed URL to retrieve sentinel object from bucket
    try {
      val signedUrl =
          serviceTemplate.exchange(endpoint + "/download/ping", HttpMethod.GET, requestEntity, String.class).getBody();
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
        log.error("Unable to connect to repository endpoint. Verify your network connection. You also need to be running on a compute node within the repository cloud.");
        throw new NotRetryableException(new IOException(
            "Access refused by repository. Ensure client is running as part of repository cloud."));
      }
      // some other unanticipated error
      throw new NotRetryableException(rce);
    } catch (NotRetryableException nre) {
      log.error(nre.getMessage());
      throw nre;
    } catch (Exception e) {
      log.error("Received unexpected exception: " + e.getMessage());
      throw new NotRetryableException(e);
    }
  }

  private HttpEntity<Object> defaultRequestEntity() {
    return new HttpEntity<Object>(defaultHeaders());
  }

  private HttpHeaders defaultHeaders() {
    val requestHeaders = new HttpHeaders();
    requestHeaders.add(HttpHeaders.USER_AGENT, clientVersion);
    return requestHeaders;
  }

}
