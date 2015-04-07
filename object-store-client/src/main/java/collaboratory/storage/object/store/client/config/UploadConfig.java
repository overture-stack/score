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
package collaboratory.storage.object.store.client.config;

import java.io.IOException;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import collaboratory.storage.object.store.client.upload.NotRetryableException;
import collaboratory.storage.object.store.client.upload.RetryableException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

@Data
@Configuration
@ConfigurationProperties(prefix = "collaboratory.upload")
public class UploadConfig {

  private String endpoint;
  private int retryNumber;
  // private int retryTimeout;
  private static final long MIN_TERNVAL = 1000;

  // TODO:
  // - http://codereview.stackexchange.com/questions/62108/efficiently-use-resttemplate-for-http-request-timeout
  // - http://stackoverflow.com/questions/13837012/spring-resttemplate-timeout
  @Bean(name = "upload-rest-template")
  public RestTemplate uploadTemplate() {
    RestTemplate req = new RestTemplate();
    req.setErrorHandler(new RetryableResponseErrorHandler());
    return req;
  }

  @Bean(name = "upload-retry-template")
  public RetryTemplate retryTemplate() {
    RetryTemplate retry = new RetryTemplate();

    int maxAttempts = retryNumber < 0 ? Integer.MAX_VALUE : retryNumber;

    Builder<Class<? extends Throwable>, Boolean> exceptions = ImmutableMap.builder();
    exceptions.put(NotRetryableException.class, Boolean.FALSE);
    exceptions.put(RetryableException.class, Boolean.TRUE);
    exceptions.put(IOException.class, Boolean.TRUE);

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(maxAttempts, exceptions.build(), true);

    // TODO: prevent DOS attack yourself
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    // long timeout = retryTimeout * 60 * 1000;
    // backOffPolicy.setBackOffPeriod(interval < MIN_TERNVAL ? MIN_TERNVAL : interval);
    retry.setBackOffPolicy(backOffPolicy);

    retry.setRetryPolicy(retryPolicy);
    return retry;

  }
}
