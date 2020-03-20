package bio.overture.score.server.security;

import lombok.val;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import static bio.overture.score.server.security.ExpiringOauth2Authentication.from;

import java.util.Map;

public class AccessTokenConverterWithExpiry extends DefaultAccessTokenConverter
{
  @Override public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
    val exp = map.get("exp");
    int expiryInSeconds = exp instanceof Integer ? (Integer) exp : 0;
    return from(super.extractAuthentication(map), expiryInSeconds);
  }
}
