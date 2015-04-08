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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.store.core.util.ChannelUtils;

@Service
@Slf4j
public class ObjectUploadServiceProxy {

  @Value("${collaboratory.upload.endpoint}")
  private String endpoint;

  @Autowired
  @Qualifier("upload-rest-template")
  private RestTemplate req;

  @Autowired
  @Qualifier("upload-retry-template")
  private RetryTemplate retry;

  public UploadProgress getProgress(String objectId) throws IOException {
    return retry.execute(new RetryCallback<UploadProgress, IOException>() {

      @Override
      public UploadProgress doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        return req.exchange(endpoint + "/upload/{object-id}/status", HttpMethod.GET,
            requestEntity,
            UploadProgress.class, objectId).getBody();
      }
    });

  }

  public void uploadPart(File file, Part part, String objectId, String uploadId) throws IOException {
    retry.execute(new RetryCallback<Void, IOException>() {

      @Override
      public Void doWithRetry(RetryContext ctx) throws IOException {
        // TODO: Change the implementation
        String etag = ChannelUtils.UploadObject(file, new URL(part.getUrl()), part.getOffset(), part.getPartSize());
        // TODO: calculate md5
        try {
          finalizeUploadPart(objectId, uploadId, part.getPartNumber(), etag, etag);
        } catch (NotRetryableException e) {
          log.warn("Checkum failed for part: {}", part, e);
          throw new RetryableException();
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
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        req.exchange(
            endpoint + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}&md5={md5}&etag={etag}",
            HttpMethod.POST, requestEntity,
            Void.class, objectId, uploadId, partNumber, md5, etag);
        return null;
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

  public boolean isUploadDataRecoverable(String objectId) throws IOException {
    return retry.execute(new RetryCallback<Boolean, IOException>() {

      @Override
      public Boolean doWithRetry(RetryContext ctx) throws IOException {
        try {
          HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
          req.exchange(
              endpoint + "/upload/{object-id}/recovery",
              HttpMethod.POST, requestEntity,
              Boolean.class, objectId);
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
    return "token";
  }

}
