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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

@Data
@Configuration
@ConfigurationProperties(prefix = "collaboratory.upload")
@Slf4j
public class UploadConfig {

  private String endpoint;
  private int retryNumber;
  private int retryInterval;
  private static final long MIN_TERNVAL = 1000;

  // TODO:
  // - http://codereview.stackexchange.com/questions/62108/efficiently-use-resttemplate-for-http-request-timeout
  // - http://stackoverflow.com/questions/13837012/spring-resttemplate-timeout
  @Bean(name = "upload-rest-template")
  public RestTemplate uploadTemplate() {
    RestTemplate req = new RestTemplate();
    return req;
  }

  @Bean(name = "upload-retry-template")
  public RetryTemplate retryTemplate() {
    RetryTemplate retry = new RetryTemplate();

    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(retryNumber < 0 ? Integer.MAX_VALUE : retryNumber);
    // TODO: set proper exception handling
    // Collection<Class> retryableExceptions = new ArrayList<Class>();
    // retryableExceptions.add(ConnectionFailureException.class);
    // retryableExceptions.add(Exception.class);
    // policy.setRetryableExceptionClasses(retryableExceptions);
    // Collection<Class> fatalExceptionClasses = new ArrayList<Class>();
    // fatalExceptionClasses.add(ResourceNotAvailableException.class);
    // policy.setFatalExceptionClasses(fatalExceptionClasses);

    // TODO: prevent DOS attack yourself
    FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
    long interval = retryInterval * 60 * 1000;
    backOffPolicy.setBackOffPeriod(interval < MIN_TERNVAL ? MIN_TERNVAL : interval);
    retry.setBackOffPolicy(backOffPolicy);

    retry.setRetryPolicy(retryPolicy);
    return retry;

  }
}
