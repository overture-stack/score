/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package collaboratory.storage.object.store.client.transport;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.client.RestTemplate;

import collaboratory.storage.object.store.client.config.ClientProperties;
import collaboratory.storage.object.store.client.download.DownloadStateStore;
import collaboratory.storage.object.store.client.exception.NotResumableException;
import collaboratory.storage.object.store.client.exception.NotRetryableException;
import collaboratory.storage.object.store.client.exception.RetryableException;
import collaboratory.storage.object.store.core.model.DataChannel;
import collaboratory.storage.object.store.core.model.ObjectSpecification;
import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.core.util.ObjectStoreUtil;

import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;

/**
 * responsible for interacting with object upload service
 */
@Service
@Slf4j
public class ObjectStoreServiceProxy {

  @Autowired
  private ClientProperties properties;

  @Autowired
  @Qualifier("object-store-service-template")
  private RestTemplate serviceRequest;

  @Autowired
  @Qualifier("object-store-template")
  private RestTemplate dataRequest;

  @Autowired
  @Qualifier("endpoint")
  private String endpoint;

  @Autowired
  @Qualifier("service-retry-template")
  private RetryTemplate retry;

  @Autowired
  private DownloadStateStore downloadStateStore;

  public UploadProgress getProgress(String objectId, long fileSize) throws IOException {
    return retry.execute(new RetryCallback<UploadProgress, IOException>() {

      @Override
      public UploadProgress doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        return serviceRequest.exchange(endpoint + "/upload/{object-id}/status?fileSize={file-size}", HttpMethod.GET,
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

        final RequestCallback callback = new RequestCallback() {

          @Override
          public void doWithRequest(final ClientHttpRequest request) throws IOException {
            // request.getHeaders().setContentLength(channel.getlength());
            request.getHeaders().set(HttpHeaders.RANGE, ObjectStoreUtil.getHttpRangeValue(part));
          }
        };

        final ResponseExtractor<HttpHeaders> headersExtractor = new ResponseExtractor<HttpHeaders>() {

          @Override
          public HttpHeaders extractData(ClientHttpResponse response) throws IOException {
            try (HashingInputStream his = new HashingInputStream(Hashing.md5(), response.getBody())) {
              channel.writeTo(his);
              response.getHeaders().set(HttpHeaders.ETAG, his.hash().toString());
              return response.getHeaders();
            }
          }
        };

        try {
          HttpHeaders headers =
              dataRequest.execute(new URI(part.getUrl()), HttpMethod.GET, callback, headersExtractor);
          part.setMd5(cleanUpETag(headers.getETag()));
          // TODO: try catch here for commit
          downloadStateStore.commit(outputDir, objectId, part);
        } catch (NotResumableException | NotRetryableException e) {
          log.error("Cannot proceed. Fail to receive part for part number: {}", part.getPartNumber(), e);
          throw e;
        } catch (Throwable e) {
          log.warn("Fail to receive part for part number: {}", part.getPartNumber(), e);
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
              dataRequest.execute(new URI(part.getUrl()), HttpMethod.PUT, callback, headersExtractor);

          try {
            finalizeUploadPart(objectId, uploadId, part.getPartNumber(), channel.getMd5(),
                cleanUpETag(headers.getETag()), disableChecksum(headers));
          } catch (NotRetryableException e) {
            log.warn("Checksum failed for part: {}, MD5: {}, ETAG: {}", part, channel.getMd5(), headers.getETag(), e);
            throw new RetryableException(e);
          }
        } catch (NotResumableException | NotRetryableException e) {
          log.error("Cannot proceed. Fail to send part for part number: {}", part.getPartNumber(), e);
          throw e;
        } catch (Throwable e) {
          log.warn("Fail to send part for part number: {}", part.getPartNumber(), e);
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

  public ObjectSpecification initiateUpload(String objectId, long length, boolean overwrite) throws IOException {
    log.debug("Initiate upload, object-id: {} overwrite: {}", objectId, overwrite);
    return retry.execute(new RetryCallback<ObjectSpecification, IOException>() {

      @Override
      public ObjectSpecification doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        return serviceRequest.exchange(
            endpoint + "/upload/{object-id}/uploads?fileSize={file-size}&overwrite={overwrite}",
            HttpMethod.POST,
            requestEntity,
            ObjectSpecification.class, objectId, length, overwrite).getBody();
      }
    });
  }

  public void finalizeDownload(File outDir, String objectId) throws IOException {
    log.debug("finalize download, object-id: {}", objectId);
    if (downloadStateStore.canFinalize(outDir, objectId)) {
      downloadStateStore.close(outDir, objectId);
    } else {
      throw new NotRetryableException(new IOException("Fail download finalization"));
    }
  }

  public void finalizeUpload(String objectId, String uploadId) throws IOException {
    log.debug("finalize upload, object-id: {}, upload-id: {}", objectId, uploadId);
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        serviceRequest.exchange(endpoint + "/upload/{object-id}?uploadId={upload-id}", HttpMethod.POST, requestEntity,
            Void.class, objectId, uploadId);
        return null;
      }
    });
  }

  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String etag,
      boolean disableChecksum)
      throws IOException {
    log.debug("finalize upload part, object-id: {}, upload-id: {}, part-number: {}", objectId, uploadId, partNumber);
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        if (disableChecksum || md5.equals(etag)) {
          HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
          serviceRequest
              .exchange(
                  endpoint
                      + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}&md5={md5}&etag={etag}",
                  HttpMethod.POST, requestEntity,
                  Void.class, objectId, uploadId, partNumber, md5, etag);
          return null;
        }
        throw new NotRetryableException();
      }
    });
  }

  public boolean isObjectExist(String objectId) throws IOException {
    log.debug("Object exists object-id: {}", objectId);
    return retry.execute(new RetryCallback<Boolean, IOException>() {

      @Override
      public Boolean doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());

        boolean result = serviceRequest.exchange(endpoint + "/upload/{object-id}",
            HttpMethod.GET, requestEntity,
            Boolean.class, objectId).getBody();
        return result;
      }
    });

  }

  public ObjectSpecification getDownloadSpecification(String objectId, long offset, long length) throws IOException {
    log.debug("Endpoint: {}", endpoint);
    return retry.execute(new RetryCallback<ObjectSpecification, IOException>() {

      @Override
      public ObjectSpecification doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        return serviceRequest.exchange(endpoint + "/download/{object-id}?offset={offset}&length={length}",
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
    log.debug("Delete part object-id: {}, upload-id: {}, part: {}", objectId, uploadId, part);
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        serviceRequest.exchange(
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
      log.warn("Download is not recoverable due to: ", e);
    }
    return false;

  }

  public boolean isUploadDataRecoverable(String objectId, long fileSize) throws IOException {
    log.debug("Recover upload, object-id: {}", objectId);
    return retry.execute(new RetryCallback<Boolean, IOException>() {

      @Override
      public Boolean doWithRetry(RetryContext ctx) throws IOException {
        try {
          HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
          serviceRequest.exchange(
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

  private HttpHeaders defaultHeaders() {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("access-token", getToken());
    return requestHeaders;
  }

  // TODO: integrate AuthorizationService
  private String getToken() {
    return properties.getKeyToken();
  }

}
