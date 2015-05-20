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
package collaboratory.storage.object.store.client.download;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import collaboratory.storage.object.store.client.config.ClientProperties;
import collaboratory.storage.object.store.core.model.ObjectSpecification;

/**
 * responsible for interacting with object upload service
 */
@Service
@Slf4j
public class ObjectDownloadServiceProxy {

  @Autowired
  private ClientProperties properties;

  @Autowired
  @Qualifier("upload-rest-template")
  private RestTemplate req;

  @Autowired
  @Qualifier("upload-data-template")
  private RestTemplate dataUploadreq;

  @Autowired
  @Qualifier("endpoint")
  private String endpoint;

  @Autowired
  @Qualifier("upload-retry-template")
  private RetryTemplate retry;

  public ObjectSpecification getDownloadSpecification(String objectId) throws IOException {
    log.debug("Endpoint: {}", endpoint);
    return retry.execute(new RetryCallback<ObjectSpecification, IOException>() {

      @Override
      public ObjectSpecification doWithRetry(RetryContext ctx) throws IOException {
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
        return req.exchange(endpoint + "/download/{object-id}", HttpMethod.GET,
            requestEntity,
            ObjectSpecification.class, objectId).getBody();
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
