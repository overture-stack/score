package bio.overture.score.server.util;

import static lombok.AccessLevel.PRIVATE;

import bio.overture.score.server.security.KeycloakPermission;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import java.util.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class Scopes {

  private static final String EXP = "exp";

  public static Set<String> extractGrantedScopes(Authentication authentication) {
    // if not OAuth2, then no scopes available at all
    Set<String> grantedScopes = Collections.emptySet();
    if (authentication instanceof JwtAuthenticationToken) {
      grantedScopes = getJwtScopes((JwtAuthenticationToken) authentication);
    } else if (authentication instanceof BearerTokenAuthentication) {
      grantedScopes = getApiKeyScopes((BearerTokenAuthentication) authentication);
    }
    return grantedScopes;
  }

  public static Set<String> extractGrantedScopesFromRpt(List<KeycloakPermission> permissionList) {
    Set<String> grantedScopes = new HashSet();

    permissionList.stream()
        .filter(perm -> perm.getScopes() != null)
        .forEach(
            permission -> {
              permission.getScopes().stream()
                  .forEach(
                      scope -> {
                        val fullScope = permission.getRsname() + "." + scope;
                        grantedScopes.add(fullScope);
                      });
            });

    return grantedScopes;
  }

  public static long extractExpiry(Map<String, ?> map) {
    Object exp = map.get(EXP);
    if (exp instanceof Integer) {
      return (Integer) exp;
    } else if (exp instanceof Long) {
      return (Long) exp;
    }
    return 0L;
  }

  private static Set<String> getJwtScopes(JwtAuthenticationToken jwt) {
    Set<String> output = new HashSet();
    try {
      val context = jwt.getToken().getClaim("context");
      if (context instanceof JSONObject) {
        val scopes = ((JSONObject) context).get("scope");
        if (scopes instanceof JSONArray) {
          val scopeArray = (JSONArray) scopes;
          scopeArray.stream()
              .filter(value -> value instanceof String)
              .forEach(value -> output.add((String) value));
        }
      }
    } catch (ClassCastException e) {
      log.debug("Received JWT not structured as expected. No scopes found.");
    }
    return output;
  }

  private static Set<String> getApiKeyScopes(BearerTokenAuthentication tokenAuthentication) {
    val scopes =
        ((OAuth2IntrospectionAuthenticatedPrincipal) tokenAuthentication.getPrincipal())
            .getScopes();
    return Set.copyOf(scopes);
  }
}
