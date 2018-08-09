package bio.overture.score.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

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
  private String encryptedAccessToken;

  private String home;
  private int retryLimit = DEFAULT_LIMIT;
  private String proxyUri;

  private String awsAccessKey;
  private String awsSecretKey;

  private int connectTimeoutSeconds;
  private int readTimeoutSeconds;

  private int parallelism = Runtime.getRuntime().availableProcessors();

  private SSLProperties ssl = new SSLProperties();

  @Data
  public static class SSLProperties {

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

}
