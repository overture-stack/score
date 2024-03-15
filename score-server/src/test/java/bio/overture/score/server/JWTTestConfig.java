package bio.overture.score.server;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@Profile("test")
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
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic())
        .signatureAlgorithm(SignatureAlgorithm.from(String.valueOf(SignatureAlgorithm.RS256)))
        .build();
  }
}
