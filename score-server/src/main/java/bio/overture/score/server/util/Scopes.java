package bio.overture.score.server.util;

import static lombok.AccessLevel.PRIVATE;

import java.util.Collections;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

@NoArgsConstructor(access = PRIVATE)
public class Scopes {

  public static Set<String> extractGrantedScopes(Authentication authentication) {
    // if not OAuth2, then no scopes available at all
    Set<String> grantedScopes = Collections.emptySet();
    if (authentication instanceof OAuth2Authentication) {
      OAuth2Authentication o2auth = (OAuth2Authentication) authentication;
      grantedScopes = getScopes(o2auth);
    }
    return grantedScopes;
  }

  private static Set<String> getScopes(OAuth2Authentication o2auth) {
    return o2auth.getOAuth2Request().getScope();
  }
}
