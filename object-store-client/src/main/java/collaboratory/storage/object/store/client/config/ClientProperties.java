package collaboratory.storage.object.store.client.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

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
  private LaunchProperties launch = new LaunchProperties();

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
  public static class LaunchProperties {

    private String ami = "TO_BE_DETERMINED";
    private String instanceType = "m3.xlarge";
    private int numberOfInstances = 1;
    private String ipRange = "0.0.0.0/0";
    private int proxyPort = 5431;
    private String vpcId;

  }
}
