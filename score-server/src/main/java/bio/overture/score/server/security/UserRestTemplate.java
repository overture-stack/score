package bio.overture.score.server.security;

import lombok.val;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

public class UserRestTemplate extends RestTemplate {
  /***
   * A Rest template for clients that need the client token (ie. SONG), not the scopes (from Ego)
   */
  public UserRestTemplate() {
    super();
    val interceptors = new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add( (HttpRequest request, byte[] body, ClientHttpRequestExecution executor) -> {
      val context = SecurityContextHolder.getContext();
      val authentication = context.getAuthentication();
      val details = authentication.getDetails();
      if (details instanceof OAuth2AuthenticationDetails) {
        val token = ((OAuth2AuthenticationDetails) details).getTokenValue();
        request.getHeaders().add("Authorization", "Bearer " + token);
      }
      return executor.execute(request,body);}
    );
    this.setInterceptors(interceptors);
  }
}
