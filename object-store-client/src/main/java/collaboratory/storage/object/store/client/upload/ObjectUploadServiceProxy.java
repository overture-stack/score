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
package collaboratory.storage.object.store.client.upload;

import java.io.IOException;
import java.net.URL;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import collaboratory.storage.object.store.client.config.ClientProperties;
import collaboratory.storage.object.store.core.model.InputChannel;
import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.store.core.util.ChannelUtils;

@Service
@Slf4j
public class ObjectUploadServiceProxy {

  private static final int MAX_TIMEOUT = 5 * 60 * 1000;

  @Value("${collaboratory.upload.endpoint}")
  private String endpoint;

  @Autowired
  private ClientProperties properties;

  @Autowired
  @Qualifier("upload-rest-template")
  private RestTemplate req;

  @Autowired
  @Qualifier("upload-retry-template")
  private RetryTemplate retry;

  public UploadProgress getProgress(String objectId, long fileSize) throws IOException {
    return retry.execute(new RetryCallback<UploadProgress, IOException>() {

      @Override
      public UploadProgress doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        return req.exchange(endpoint + "/upload/{object-id}/status?fileSize={file-size}", HttpMethod.GET,
            requestEntity,
            UploadProgress.class, objectId, fileSize).getBody();
      }
    });

  }

  public void uploadPart(InputChannel channel, Part part, String objectId, String uploadId) throws IOException {
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        // TODO: Change the implementation
        log.debug("URL: {}", part.getUrl());
        try {
          String etag = ChannelUtils.UploadObject(channel, new URL(part.getUrl()));
          try {
            finalizeUploadPart(objectId, uploadId, part.getPartNumber(), channel.getMd5(), etag);
          } catch (NotRetryableException e) {
            log.warn("Checkum failed for part: {}", part, e);
            throw new RetryableException();
          }
        } catch (Exception e) {
          log.warn("Fail to send part for part number: {}", part.getPartNumber(), e);
          channel.reset();
          throw e;
        }
        return null;
      }
    });
  }

  public UploadSpecification initiateUpload(String objectId, long length) throws IOException {
    return retry.execute(new RetryCallback<UploadSpecification, IOException>() {

      @Override
      public UploadSpecification doWithRetry(RetryContext ctx) throws IOException {
        log.debug("Retry #: {}", ctx.getRetryCount());
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        return req.exchange(endpoint + "/upload/{object-id}/uploads?fileSize={file-size}", HttpMethod.POST,
            requestEntity,
            UploadSpecification.class, objectId, length).getBody();
      }
    });
  }

  public void finalizeUpload(String objectId, String uploadId) throws IOException {

    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        req.exchange(endpoint + "/upload/{object-id}?uploadId={upload-id}", HttpMethod.POST, requestEntity,
            Void.class, objectId, uploadId);
        return null;
      }
    });
  }

  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String etag)
      throws IOException {
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        if (md5.equals(etag)) {
          HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
          req.exchange(
              endpoint + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}&md5={md5}&etag={etag}",
              HttpMethod.POST, requestEntity,
              Void.class, objectId, uploadId, partNumber, md5, etag);
          return null;
        }
        throw new NotRetryableException();
      }
    });
  }

  public boolean isObjectExist(String objectId) throws IOException {
    return retry.execute(new RetryCallback<Boolean, IOException>() {

      @Override
      public Boolean doWithRetry(RetryContext ctx) throws IOException {
        try {
          HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
          req.exchange(
              endpoint + "/upload/{object-id}",
              HttpMethod.GET, requestEntity,
              Boolean.class, objectId);
        } catch (Exception e) {
          return false;
        }
        return true;
      }
    });
  }

  public void deletePart(String objectId, String uploadId, Part part) throws IOException {
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        req.exchange(
            endpoint + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}",
            HttpMethod.DELETE, requestEntity,
            Void.class, objectId, uploadId, part.getPartNumber());
        return null;
      }
    });
  }

  public boolean isUploadDataRecoverable(String objectId, long fileSize) throws IOException {
    return retry.execute(new RetryCallback<Boolean, IOException>() {

      @Override
      public Boolean doWithRetry(RetryContext ctx) throws IOException {
        try {
          HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
          req.exchange(
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

  private ClientHttpRequestFactory clientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setReadTimeout(MAX_TIMEOUT);
    factory.setConnectTimeout(MAX_TIMEOUT);
    return factory;
  }
}
