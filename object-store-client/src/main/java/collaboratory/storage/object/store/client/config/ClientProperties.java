package collaboratory.storage.object.store.client.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Configurations for user profile and preferences
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "client")
public class ClientProperties {

  /**
   * Constants.
   */
  private static final int DEFAULT_LIMIT = 10;

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

  private boolean strictSsl = true;

  @Data
  public static class TokenProperties {

    private String accessTokenUri;
    private String resourceId;
    private String clientId;
    private String clientSecret;

  }

  @Data
  public static class SSLProperties {

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
