package bio.overture.score.server.config;

import java.net.URI;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@Profile("secure")
@Getter
public class KeycloakConfig {

  @Value("${auth.server.clientID}")
  private String uma_audience;

  @Value("${auth.server.keycloak.host}")
  private String host;

  @Value("${auth.server.keycloak.realm}")
  private String realm;

  private static final String UMA_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:uma-ticket";
  private static final String UMA_RESPONSE_MODE = "permissions";

  public URI permissionUrl() {
    return UriComponentsBuilder.fromHttpUrl(host)
        .pathSegment("realms", realm, "protocol/openid-connect/token")
        .build()
        .toUri();
  }

  public MultiValueMap<String, String> getUmaParams() {
    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("grant_type", UMA_GRANT_TYPE);
    map.add("audience", uma_audience);
    map.add("response_mode", UMA_RESPONSE_MODE);
    return map;
  }
}
