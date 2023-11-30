package bio.overture.score.server.utils;

import static bio.overture.score.server.utils.JsonUtils.toJson;
import static bio.overture.score.server.utils.JsonUtils.toMap;
import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.HOURS;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.KeyPair;
import java.util.Date;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"test", "jwt"})
public class JWTGenerator {

  public static final String DEFAULT_ISSUER = "ego";
  public static final String DEFAULT_ID = "68418f9f-65b9-4a17-ac1c-88acd9984fe0";
  public static final String DEFAULT_SUBJECT = "none";
  private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.RS256;

  private final KeyPair keyPair;

  @Autowired
  public JWTGenerator(@NonNull KeyPair keyPair) {
    this.keyPair = keyPair;
  }

  public String generateJwtNoContext(boolean expired) {
    return generate(calcTTLMs(expired), null);
  }

  public String generateJwtWithContext(JwtContext jwtContext, boolean expired) {
    return generate(calcTTLMs(expired), jwtContext);
  }

  @SneakyThrows
  public Jws<Claims> verifyAndGetClaims(String jwtString) {
    val publicKey = keyPair.getPublic();
    return Jwts.parser().setSigningKey(publicKey).parseClaimsJws(jwtString);
  }

  private static long calcTTLMs(boolean expired) {
    return expired ? 0 : HOURS.toMillis(5);
  }

  @SneakyThrows
  private String generate(long ttlMs, JwtContext jwtContext) {
    long nowMs = System.currentTimeMillis();

    long expiry;
    // if ttlMs <= 0 make it expired
    if (ttlMs <= 0) {
      expiry = nowMs - 10000;
      nowMs -= 100000L;
    } else {
      expiry = nowMs + ttlMs;
    }

    val decodedPrivateKey = keyPair.getPrivate();
    val jwtBuilder =
        Jwts.builder()
            .setId(DEFAULT_ID)
            .setIssuedAt(new Date(nowMs))
            .setSubject(DEFAULT_SUBJECT)
            .setIssuer(DEFAULT_ISSUER)
            .setExpiration(new Date(expiry))
            .signWith(SIGNATURE_ALGORITHM, decodedPrivateKey);
    if (!isNull(jwtContext)) {
      jwtBuilder.addClaims(toMap(toJson(jwtContext)));
    }
    return jwtBuilder.compact();
  }
}
