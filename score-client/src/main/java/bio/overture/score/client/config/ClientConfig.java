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
package bio.overture.score.client.config;

import bio.overture.score.client.download.DownloadStateStore;
import bio.overture.score.client.exception.AmazonS3RetryableResponseErrorHandler;
import bio.overture.score.client.exception.ConnectivityResponseHandler;
import bio.overture.score.client.exception.NotResumableException;
import bio.overture.score.client.exception.NotRetryableException;
import bio.overture.score.client.exception.RetryableException;
import bio.overture.score.client.exception.ServiceRetryableResponseErrorHandler;
import bio.overture.score.client.manifest.kf.KFFileBean;
import bio.overture.score.client.upload.UploadStateStore;
import bio.overture.score.client.util.CsvParser;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;

import static com.google.common.base.Objects.firstNonNull;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * Configurations for connections for uploads
 */
@Slf4j
@Configuration
@EnableConfigurationProperties
@Import(PropertyPlaceholderAutoConfiguration.class)
public class ClientConfig {

  private static final Character TAB_SEP = '\t';
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
  private HostnameVerifier hostnameVerifier;

  @Bean
  public String clientVersion() {
    return firstNonNull(ClientConfig.class.getPackage().getImplementationVersion(), "[unknown version]");
  }

  @Bean
  public CsvParser<KFFileBean> kfFileBeanCsvParser(){
    return new CsvParser<>(KFFileBean.class, TAB_SEP);
  }

  @Bean
  public TemplateEngine textTemplateEngine(){
    val templateEngine = new TemplateEngine();
    templateEngine.addTemplateResolver(springThymeleafTemplateResolver());
    return templateEngine;
  }

  @Bean
  public ITemplateResolver springThymeleafTemplateResolver() {
    val templateResolver = new SpringResourceTemplateResolver();
    templateResolver.setPrefix("classpath:/templates/");
    templateResolver.setSuffix(".txt");
    templateResolver.setTemplateMode(TemplateMode.TEXT);
    templateResolver.setCharacterEncoding("UTF8");
    templateResolver.setCheckExistence(true);
    templateResolver.setCacheable(false);
    templateResolver.setOrder(1);
    return templateResolver;
  }

  @Bean
  public DownloadStateStore downloadStateStore() {
    return new DownloadStateStore();
  }

  @Bean
  public UploadStateStore uploadStateStore() {
    return new UploadStateStore();
  }

  @Bean
  public RestTemplate serviceTemplate() {
    val serviceTemplate = new RestTemplate(clientHttpRequestFactory());
    serviceTemplate.setErrorHandler(new ServiceRetryableResponseErrorHandler());

    return serviceTemplate;
  }

  @Bean
  public RestTemplate dataTemplate() {
    val dataTemplate = new RestTemplate(streamingClientHttpRequestFactory());
    dataTemplate.setErrorHandler(new AmazonS3RetryableResponseErrorHandler());

    return dataTemplate;
  }

  @Bean
  public RestTemplate pingTemplate() {
    val pingTemplate = new RestTemplate(pingHttpRequestFactory());
    pingTemplate.setErrorHandler(new ConnectivityResponseHandler());

    return pingTemplate;
  }

  @Bean
  public RetryTemplate retryTemplate(
      @Value("${storage.retryNumber}") int retryNumber,
      @Value("${storage.retryTimeout}") int retryTimeout) {
    val maxAttempts = retryNumber < 0 ? Integer.MAX_VALUE : retryNumber;

    val exceptions = ImmutableMap.<Class<? extends Throwable>, Boolean> builder();
    exceptions.put(Error.class, Boolean.FALSE);
    exceptions.put(NotResumableException.class, Boolean.FALSE);
    exceptions.put(NotRetryableException.class, Boolean.FALSE);
    exceptions.put(RetryableException.class, Boolean.TRUE);
    exceptions.put(IOException.class, Boolean.TRUE);

    val retryPolicy = new SimpleRetryPolicy(maxAttempts, exceptions.build(), true);
    val backOffPolicy = new ExponentialBackOffPolicy();

    val retry = new RetryTemplate();
    retry.setBackOffPolicy(backOffPolicy);
    retry.setRetryPolicy(retryPolicy);

    return retry;
  }

  private HttpComponentsClientHttpRequestFactory pingHttpRequestFactory() {
    val factory = new HttpComponentsClientHttpRequestFactory();

    // HttpComponentsClientHttpRequestFactory *may* ignore these, but lets do it anyways in hopes
    // to maximize the number of places that it may be used elsewhere
    configureSystemHttpTimeouts();

    factory.setConnectTimeout(5000);
    factory.setReadTimeout(5000);

    return factory;
  }

  /**
   * Request Factory that contains security headers
   */
  private HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
    val factory = new HttpComponentsClientHttpRequestFactory();

    // HttpComponentsClientHttpRequestFactory *may* ignore these, but lets do it anyways in hopes
    // to maximize the number of places that it may be used elsewhere
    configureSystemHttpTimeouts();

    factory.setConnectTimeout(properties.getConnectTimeoutSeconds() * 1000);
    factory.setReadTimeout(properties.getReadTimeoutSeconds() * 1000);

    factory.setHttpClient(secureClient());

    return factory;
  }

  private SimpleClientHttpRequestFactory streamingClientHttpRequestFactory() {
    val factory = new SimpleClientHttpRequestFactory();

    // SimpleClientHttpRequestFactory *will 100%* ignore these, but lets do it anyways in hopes
    // to maximize the number of places that it may be used elsewhere
    configureSystemHttpTimeouts();

    // https://jira.oicr.on.ca/browse/COL-487
    factory.setConnectTimeout(properties.getConnectTimeoutSeconds() * 1000);
    factory.setReadTimeout(properties.getReadTimeoutSeconds() * 1000);

    factory.setOutputStreaming(true);
    factory.setBufferRequestBody(false);

    return factory;
  }

  @SneakyThrows
  /**
   * @return instance of HttpClient with SSL and OAuth configuration
   */
  private HttpClient secureClient() {
    val client = HttpClients.custom();
    client.setSSLContext(sslContext);
    client.setSSLHostnameVerifier(hostnameVerifier);
    configureOAuth(client);

    return client.build();
  }

  /**
   * Configure JVM wide timeouts of HTTP sockets.
   * <p>
   * May not be respected by all library implementors.
   * 
   * @see http://stackoverflow.com/questions/9934970/can-i-globally-set-the-timeout-of-http-connections#answer-10705424
   */
  private void configureSystemHttpTimeouts() {
    // These lines are ignored by SimpleClientHttpRequestFactory
    System.setProperty("sun.net.client.defaultConnectTimeout",
        Long.toString(properties.getConnectTimeoutSeconds() * 1000));
    System.setProperty("sun.net.client.defaultReadTimeout",
        Long.toString(properties.getReadTimeoutSeconds() * 1000));
  }

  /**
   * Populates Authorization header with OAuth token
   * @param HttpClientBuilder instance to set Default Header on. Existing Headers will be overwritten.
   */
  private void configureOAuth(HttpClientBuilder client) {
    val accessToken = properties.getAccessToken();

    val defined = (accessToken != null) && (!accessToken.isEmpty());
    if (defined) {
      log.trace("Setting access token: {}", accessToken);
      client.setDefaultHeaders(singletonList(new BasicHeader(AUTHORIZATION, format("Bearer %s", accessToken))));
    } else {
      // Omit header if access token is null/empty in configuration (application.yml and conf/properties file
      log.warn("No access token specified");
    }
  }

}
