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

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

import java.io.IOException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import collaboratory.storage.object.store.client.download.DownloadStateStore;
import collaboratory.storage.object.store.client.exception.AmazonS3RetryableResponseErrorHandler;
import collaboratory.storage.object.store.client.exception.NotResumableException;
import collaboratory.storage.object.store.client.exception.NotRetryableException;
import collaboratory.storage.object.store.client.exception.RetryableException;
import collaboratory.storage.object.store.client.exception.ServiceRetryableResponseErrorHandler;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Configurations for connections for uploads
 */
@Slf4j
@Data
@Configuration
public class ObjectStoreConfig {

  /**
   * Constants.
   */
  private static final int MAX_TIMEOUT = 20 * 1000;

  /**
   * Configuration.
   */
  @Autowired
  private ClientProperties properties;

  /**
   * Dependencies.
   */
  @Autowired
  private SSLContext sslContext;
  @Autowired
  private X509HostnameVerifier hostnameVerifier;

  @Bean
  public DownloadStateStore downloadStateStore() {
    return new DownloadStateStore();
  }

  @Bean(name = "object-store-service-template")
  public RestTemplate uploadTemplate() {
    RestTemplate req = new RestTemplate(clientHttpRequestFactory());
    req.setErrorHandler(new ServiceRetryableResponseErrorHandler());
    return req;
  }

  @Bean(name = "object-store-template")
  public RestTemplate uploadDataTemplate() {
    RestTemplate req = new RestTemplate(streamingClientHttpRequestFactory());
    req.setErrorHandler(new AmazonS3RetryableResponseErrorHandler());
    return req;
  }

  private HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setReadTimeout(MAX_TIMEOUT);
    factory.setConnectTimeout(MAX_TIMEOUT);
    factory.setHttpClient(sslClient());

    return factory;
  }

  @SneakyThrows
  private HttpClient sslClient() {
    val client = HttpClients.custom();
    client.setSslcontext(sslContext);
    client.setHostnameVerifier(hostnameVerifier);
    configureOAuth(client);

    return client.build();
  }

  private SimpleClientHttpRequestFactory streamingClientHttpRequestFactory() {
    val factory = new SimpleClientHttpRequestFactory();
    factory.setReadTimeout(MAX_TIMEOUT);
    factory.setConnectTimeout(MAX_TIMEOUT);
    factory.setOutputStreaming(true);
    factory.setBufferRequestBody(false);
    return factory;

  }

  @Bean(name = "service-retry-template")
  public RetryTemplate retryTemplate() {
    RetryTemplate retry = new RetryTemplate();

    int maxAttempts =
        properties.getUpload().getRetryNumber() < 0 ? Integer.MAX_VALUE : properties.getUpload().getRetryNumber();

    Builder<Class<? extends Throwable>, Boolean> exceptions = ImmutableMap.builder();
    exceptions.put(Error.class, Boolean.FALSE);
    exceptions.put(NotResumableException.class, Boolean.FALSE);
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

  @Bean(name = "endpoint")
  public String endpoint() {
    val scheme = properties.getSsl().isEnabled() ? "https" : "http";
    return scheme + "://" + properties.getUpload().getServiceHostname() + ":" + properties.getUpload().getServicePort();
  }

  private void configureOAuth(HttpClientBuilder client) {
    val accessToken = properties.getAccessToken();

    val defined = accessToken != null;
    if (defined) {
      log.debug("Setting access token: {}", accessToken);
      client.setDefaultHeaders(singletonList(new BasicHeader(AUTHORIZATION, format("Bearer %s", accessToken))));
    }
  }

}
