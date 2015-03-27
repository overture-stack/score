package collaboratory.storage.object.store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * S3 configuration.
 */
@Configuration
public class S3Config {

  @Value("${upload.retry.limit}")
  private int retryLimit;

  @Bean
  public AmazonS3 s3() {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setProtocol(Protocol.HTTPS);
    clientConfiguration.setRetryPolicy(PredefinedRetryPolicies
        .getDefaultRetryPolicyWithCustomMaxRetries(retryLimit));
    return new AmazonS3Client(clientConfiguration);
  }

}
