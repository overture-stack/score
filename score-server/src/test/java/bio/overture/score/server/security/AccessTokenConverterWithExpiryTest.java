package bio.overture.score.server.security;

import lombok.val;
import org.junit.Test;

import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccessTokenConverterWithExpiryTest {
  @Test
  public void test_sets_expiry() {
    val expiry = 1000;
    val sut = new AccessTokenConverterWithExpiry();
    val inputs = new TreeMap<String, Object>();

    val scopes = Set.of("test.TEST1-CA.download", "test.TEST1-CA.upload");
    inputs.put(sut.EXP, expiry);
    inputs.put(sut.SCOPE, scopes);

    val auth = sut.extractAuthentication(inputs);
    assertTrue(auth instanceof ExpiringOauth2Authentication);

    val expiringAuth = (ExpiringOauth2Authentication) auth;
    val exp = expiringAuth.getExpiry();

    assertEquals(expiry, exp);
    assertEquals(scopes, expiringAuth.getOAuth2Request().getScope());
  }
}
