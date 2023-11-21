package bio.overture.score.server.security;

import bio.overture.score.server.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionAuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

@Slf4j
@AllArgsConstructor
public class ApiKeyIntrospector implements OpaqueTokenIntrospector {

  private String introspectionUri;
  private String clientId;
  private String clientSecret;
  private String tokenName;

  @Override
  public OAuth2AuthenticatedPrincipal introspect(String token) {

    // Add token to body
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add(tokenName, token);

    // URI format
    val uriWithToken =
        UriComponentsBuilder.fromHttpUrl(introspectionUri)
            .build()
            .toUri();

    // Get response from Auth Server
    val template = new RestTemplate();
    template.setErrorHandler(new RestTemplateResponseErrorHandler());
    val response =
        template.postForEntity(
            uriWithToken, new HttpEntity<>(formData, getBasicAuthHeader()), JsonNode.class);

    // Ensure response was OK
    if ((response.getStatusCode() != HttpStatus.OK
        && response.getStatusCode() != HttpStatus.MULTI_STATUS
        && response.getStatusCode() != HttpStatus.UNAUTHORIZED)
        || !response.hasBody()) {
      throw new OAuth2IntrospectionException("Bad Response from Ego Server");
    }

    val responseBody = response.getBody();

    val isValid = validateIntrospectResponse(response.getStatusCode(), responseBody);
    if (!isValid) {
      throw new BadOpaqueTokenException("ApiKey is revoked or expired.");
    }

    // ApiKey check is successful. Build authenticated principal and return.
    return convertResponseToPrincipal(responseBody);
  }

  private HttpHeaders getBasicAuthHeader() {
    val headers = new HttpHeaders();
    headers.setBasicAuth(clientId, clientSecret);
    headers.set(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE);
    return headers;
  }

  private boolean validateIntrospectResponse(HttpStatus status, JsonNode response) {
    if (response.has("error")) {
      log.debug("Check Token response includes an error: {}", response.has("error"));
      return false;
    }
    if (status != HttpStatus.OK && status != HttpStatus.MULTI_STATUS) {
      log.debug(
          "Check Token response is unauthorized but does not list the error. Rejecting token.");
      return false;
    }

    if(response.has("exp") && response.get("exp").asLong() == 0){
      log.debug("Token is expired. Rejecting token.");
      return false;
    }

    if(response.has("revoked") && response.get("revoked").asBoolean() == true){
      log.debug("Token is revoked. Rejecting token.");
      return false;
    }

    if(response.has("valid") && response.get("valid").asBoolean() == false){
      log.debug("Check Token response 'valid' field is false. Rejecting token.");
      return false;
    }

    return true;
  }

  private OAuth2AuthenticatedPrincipal convertResponseToPrincipal(JsonNode responseJson) {
    val response = JsonUtils.convertValue(responseJson, ApiKeyIntrospectResponse.class);

    Collection<GrantedAuthority> authorities = new ArrayList();
    Map<String, Object> claims = new HashMap<>();

    if (!response.getScope().isEmpty()) {
      List<String> scopes = Collections.unmodifiableList(response.getScope());
      claims.put("scope", scopes);
      val var5 = scopes.iterator();

      while (var5.hasNext()) {
        String scope = (String) var5.next();
        authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
      }
    }

    return new OAuth2IntrospectionAuthenticatedPrincipal(claims, authorities);
  }

  public class RestTemplateResponseErrorHandler
      implements ResponseErrorHandler {

    @Override
    public boolean hasError(ClientHttpResponse httpResponse)
        throws IOException {

      return (
          httpResponse.getStatusCode().series() == CLIENT_ERROR
              || httpResponse.getStatusCode().series() == SERVER_ERROR);
    }

    @Override
    public void handleError(ClientHttpResponse httpResponse)
        throws IOException {

      if (httpResponse.getStatusCode().series() == CLIENT_ERROR) {
        // throw 401 HTTP error code
        throw new BadCredentialsException(httpResponse.getStatusText());
      } else {
        // throw 500 HTTP error code
        throw new OAuth2IntrospectionException(httpResponse.getStatusText());
      }
    }
  }
}
