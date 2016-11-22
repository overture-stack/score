package org.icgc.dcc.storage.server.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.SignerFactory;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.internal.S3Signer;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.SSEAlgorithm;

/**
 * S3/Ceph Object Gateway configuration.
 */
@Data
@Slf4j
@Configuration
@Profile({ "aws", "collaboratory", "default" })
@ConfigurationProperties(prefix = "s3")
public class S3Config {

  @Value("${upload.retry.limit}")
  private int retryLimit;

  private String accessKey;
  private String secretKey;
  private String endpoint;
  private boolean isSecured;
  private String masterEncryptionKeyId;

  @Value("${upload.connection.timeout}")
  private int connectionTimeout;

  @Bean
  public AmazonS3 s3() {
    AmazonS3 s3Client = null;
    if (accessKey != null && secretKey != null) {
      s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey), clientConfiguration());
    } else {
      s3Client = new AmazonS3Client(new ProfileCredentialsProvider(), clientConfiguration());
    }

    log.debug("Endpoint: {}", endpoint);
    log.debug("Retries: {}", retryLimit);
    log.debug("Timeout: {}", connectionTimeout);
    s3Client.setEndpoint(endpoint);
    s3Client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));

    return s3Client;
  }

  private ClientConfiguration clientConfiguration() {
    ClientConfiguration clientConfiguration = new ClientConfiguration();

    log.info("master key id : '{}'", masterEncryptionKeyId);
    if (isEncryptionEnabled()) {
      clientConfiguration.setSignerOverride("AWSS3V4SignerType");
      log.info("Using AWSS3V4SignerType");
    } else {
      SignerFactory.registerSigner("S3Signer", S3Signer.class);
      clientConfiguration.setSignerOverride("S3SignerType");
      log.info("Using S3SignerType");
    }

    if (isSecured) {
      clientConfiguration.setProtocol(Protocol.HTTPS);
    } else {
      clientConfiguration.setProtocol(Protocol.HTTP);
    }
    clientConfiguration.setRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(retryLimit));
    clientConfiguration.setConnectionTimeout(connectionTimeout);
    return clientConfiguration;
  }

  public void encrypt(InitiateMultipartUploadRequest req) {
    if (isEncryptionEnabled()) {
      log.debug("Encryption is on. Key: {}", masterEncryptionKeyId);
      req.putCustomRequestHeader(Headers.SERVER_SIDE_ENCRYPTION, SSEAlgorithm.KMS.getAlgorithm());
      req.putCustomRequestHeader(Headers.SERVER_SIDE_ENCRYPTION_AWS_KMS_KEYID, masterEncryptionKeyId);
    }
  }

  private boolean isEncryptionEnabled() {
    return masterEncryptionKeyId != null && !masterEncryptionKeyId.isEmpty();
  }

}