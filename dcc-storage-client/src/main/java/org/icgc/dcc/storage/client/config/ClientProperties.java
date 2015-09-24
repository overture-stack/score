package org.icgc.dcc.storage.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configurations for user profile and preferences
 */
@Data
@Component
@ConfigurationProperties(prefix = "client")
public class ClientProperties {

  /**
   * Constants.
   */
  private static final int DEFAULT_LIMIT = 10;

  /** OAuth2 access token for ICGC authorization server */
  private String accessToken;

  private String home;
  private int retryLimit = DEFAULT_LIMIT;
  private String proxyUri;

  private String awsAccessKey;
  private String awsSecretKey;

  private String osEndpoint;
  private String keyStoneEndPoint;

  private String keyToken;
  private String keyResourceUri;

  private String baiCacheDir;
  private String uploadTmpDir;
  private int parallelism = Runtime.getRuntime().availableProcessors();

  private TokenProperties token = new TokenProperties();
  private SSLProperties ssl = new SSLProperties();
  private UploadProperties upload = new UploadProperties();

  @Data
  public static class TokenProperties {

    private String accessTokenUri;
    private String resourceId;
    private String clientId;
    private String clientSecret;

  }

  @Data
  public static class SSLProperties {

    /** Use SSL for communication with the server? */
    private boolean enabled = true;

    /** Use custom SSL configuration including trust store and host verifier? */
    private boolean custom = true;

    /*
     * Properties used when {@code custom = true}:
     */

    private Resource trustStore;
    private String trustStoreType;
    private String trustStorePassword;
    private String trustName;

    private Resource keyStore;
    private String keyStoreType;
    private String keyStorePassword;
    private String keyAlias;

  }

  @Data
  public static class UploadProperties {

    private String serviceHostname = "TO_BE_DETERMINED";
    private int servicePort = 5431;
    private int retryNumber = 1;
    private int retryTimeout = 1;

  }
}
