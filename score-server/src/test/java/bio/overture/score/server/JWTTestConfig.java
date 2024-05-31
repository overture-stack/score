package bio.overture.score.server;

import bio.overture.score.server.security.PublicKeyFetcher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test & jwt")
public class JWTTestConfig {

  private final KeyPair keyPair;

  @SneakyThrows
  public JWTTestConfig() {
    val keyGenerator = KeyPairGenerator.getInstance("RSA");
    keyGenerator.initialize(2048);
    this.keyPair = keyGenerator.genKeyPair();
  }

  @Bean
  @Primary
  public KeyPair keyPair() {
    return keyPair;
  }

  @Bean
  @Primary
  public PublicKeyFetcher testPublicKeyFetcher() {
    return this::getPublicKey;
  }

  public String getPublicKey() {
    return convertToPublicKeyWithHeader(getDecodedPublicKey());
  }

  public String getDecodedPublicKey() {
    return Base64.getEncoder().encodeToString(keyPair().getPublic().getEncoded());
  }

  private static String convertToPublicKeyWithHeader(String key) {
    val result = new StringBuilder();
    result.append("-----BEGIN PUBLIC KEY-----\n");
    result.append(key);
    result.append("\n-----END PUBLIC KEY-----");
    return result.toString();
  }
}
