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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.store.core.util.ChannelUtils;

@Service
public class ObjectUploadServiceProxy {

  @Value("${collaboratory.upload.endpoint}")
  private String endpoint;

  @Autowired
  @Qualifier("upload-template")
  private RestTemplate req;

  @Retryable(maxAttempts = 100, backoff = @Backoff(delay = 100, maxDelay = 500))
  public UploadProgress getProgress(String objectId) {
    HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
    return req.exchange(endpoint + "/upload/{object-id}", HttpMethod.GET,
        requestEntity,
        UploadProgress.class, objectId).getBody();

  }

  @Retryable(maxAttempts = 100, backoff = @Backoff(delay = 100, maxDelay = 500))
  public String uploadPart(File file, Part part) throws IOException {
    return ChannelUtils.UploadObject(file, new URL(part.getUrl()), part.getOffset(), part.getPartSize());
  }

  @Retryable(maxAttempts = 100, backoff = @Backoff(delay = 100, maxDelay = 500))
  public UploadSpecification initiateUpload(String objectId, long length) {
    HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
    return req.exchange(endpoint + "/upload/{object-id}/uploads?fileSize={file-size}", HttpMethod.POST, requestEntity,
        UploadSpecification.class, objectId, length).getBody();
  }

  @Retryable(maxAttempts = 100, backoff = @Backoff(delay = 100, maxDelay = 500))
  public void finalizeUpload(String objectId, String uploadId) {

    HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
    req.exchange(endpoint + "/upload/{object-id}?uploadId={upload-id}", HttpMethod.POST, requestEntity,
        Void.class, objectId, uploadId);
  }

  @Retryable(maxAttempts = 100, backoff = @Backoff(delay = 100, maxDelay = 500))
  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String etag) {
    HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());

    req.exchange(
        endpoint + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}&md5={md5}&etag={etag}",
        HttpMethod.POST, requestEntity,
        Void.class, objectId, uploadId, partNumber, md5, etag);
  }

  private HttpHeaders defaultHeaders() {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("access-token", getToken());
    return requestHeaders;
  }

  private String getToken() {
    return "token";

  }
}
