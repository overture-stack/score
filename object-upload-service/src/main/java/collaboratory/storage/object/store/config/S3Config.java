package collaboratory.storage.object.store.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;

/**
 * S3/Ceph Object Gateway configuration.
 */
@Configuration
@Data
@Slf4j
@ConfigurationProperties(prefix = "s3")
public class S3Config {

  @Value("${upload.retry.limit}")
  private int retryLimit;

  private String accessKey;
  private String secretKey;
  private String endpoint;
  private boolean isSecured;

  @Bean
  public AmazonS3 s3() {
    AmazonS3 s3Client = null;
    if (accessKey != null && secretKey != null) {
      s3Client = new AmazonS3Client(new BasicAWSCredentials(
          accessKey, secretKey),
          clientConfiguration());
    } else {
      s3Client = new AmazonS3Client(new ProfileCredentialsProvider(), clientConfiguration());
    }

    if (endpoint != null && !endpoint.isEmpty()) {
      log.debug("OS Endpoint: {}", endpoint);
      s3Client.setEndpoint(endpoint);
    } else {
      s3Client.setEndpoint("s3-external-1.amazonaws.com");
    }
    s3Client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));

    return s3Client;
  }

  private ClientConfiguration clientConfiguration() {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    if (!isSecured) {
      clientConfiguration.setProtocol(Protocol.HTTP);
    }
    clientConfiguration
        .setRetryPolicy(PredefinedRetryPolicies
            .getDefaultRetryPolicyWithCustomMaxRetries(retryLimit));
    return clientConfiguration;

  }
}