package bio.overture.score.server.repository.auth;

import bio.overture.score.server.config.KeycloakConfig;
import bio.overture.score.server.security.KeycloakPermission;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpStatus.Series.CLIENT_ERROR;
import static org.springframework.http.HttpStatus.Series.SERVER_ERROR;

@Slf4j
@Service
public class KeycloakAuthorizationService {

  private final KeycloakConfig keycloakConfig;

  public KeycloakAuthorizationService(@Autowired KeycloakConfig keycloakConfig) {
    this.keycloakConfig = keycloakConfig;
  }

  public List<KeycloakPermission> fetchAuthorizationGrants(String accessToken){

    val serviceUrl = keycloakConfig.permissionUrl();

    HttpEntity<MultiValueMap<String, String>> request =
        new HttpEntity<>(keycloakConfig.getUmaParams(), getBearerAuthHeader(accessToken));

    ResponseEntity<KeycloakPermission[]> response;

    try {
      // Get response from Keycloak
      val template = new RestTemplate();
      template.setErrorHandler(new RestTemplateResponseErrorHandler());
      response =
          template.postForEntity(
              serviceUrl, request, KeycloakPermission[].class);
    } catch (ResourceAccessException e) {
      log.error(
          "KeycloakAuthorizationService - error cause:"
              + e.getCause()
              + " message:"
              + e.getMessage());
      throw new OAuth2IntrospectionException("Bad Response from Keycloak Server");
    }

    // Ensure response was OK
    if ((response.getStatusCode() != HttpStatus.OK
        && response.getStatusCode() != HttpStatus.MULTI_STATUS
        && response.getStatusCode() != HttpStatus.UNAUTHORIZED)
        || !response.hasBody()) {
      throw new OAuth2IntrospectionException("Bad Response from Keycloak Server");
    }

    val isValid = validateIntrospectResponse(response.getStatusCode());
    if (!isValid) {
      throw new BadOpaqueTokenException("ApiKey is revoked or expired.");
    }

    return List.of(response.getBody());
  }

  private HttpHeaders getBearerAuthHeader(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setBearerAuth(token);
    return headers;
  }

  private boolean validateIntrospectResponse(HttpStatus status) {
    if (status != HttpStatus.OK && status != HttpStatus.MULTI_STATUS) {
      log.debug(
          "Check Token response is unauthorized but does not list the error. Rejecting token.");
      return false;
    }
    return true;
  }

  private static class RestTemplateResponseErrorHandler
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
