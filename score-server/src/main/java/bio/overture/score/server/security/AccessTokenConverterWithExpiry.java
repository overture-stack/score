package bio.overture.score.server.security;

import lombok.val;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import static bio.overture.score.server.security.ExpiringOauth2Authentication.from;

import java.util.Map;

/***
 * RemoteTokenServices uses a postForMap call to convert the Oauth2 JSON response that we get from Ego into a Java
 * Map from string to an unknown object type.
 *
 * The default converter just extracts the scope field; we also want to extract the "exp" field, which holds the time
 * to expiry for our token in seconds.
 *
 */
public class AccessTokenConverterWithExpiry extends DefaultAccessTokenConverter
{
  @Override public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
    val exp = map.get("exp");
    val expiryInSeconds = (exp != null && exp instanceof Integer ) ? (Integer) exp : 0;
    return from(super.extractAuthentication(map), expiryInSeconds);
  }
}