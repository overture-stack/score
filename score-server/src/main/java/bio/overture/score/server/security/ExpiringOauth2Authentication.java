package bio.overture.score.server.security;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

@Getter
public class ExpiringOauth2Authentication extends OAuth2Authentication {
  private final long expiry; // expiry time of the authentication token in seconds

  public ExpiringOauth2Authentication(OAuth2Request storedRequest,
    Authentication userAuthentication, long expiry) {
    super(storedRequest, userAuthentication);
    this.expiry = expiry;
  }

  public static ExpiringOauth2Authentication from(OAuth2Authentication existing, long expiry) {
    return new ExpiringOauth2Authentication(existing.getOAuth2Request(), existing.getUserAuthentication(), expiry);
  }
}
