package bio.overture.score.server.security;

import com.google.common.base.Strings;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.util.UUID;


public class MergedServerTokenServices implements ResourceServerTokenServices {
    private final DefaultTokenServices jwtTokenService;
    private final RemoteTokenServices remoteTokenServices;

    public MergedServerTokenServices(DefaultTokenServices jwtTokenService, RemoteTokenServices remoteTokenServices) {
        this.jwtTokenService = jwtTokenService;
        this.remoteTokenServices = remoteTokenServices;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        if (isApiKey(accessToken)) {
            return remoteTokenServices.loadAuthentication(accessToken);
        }
        return jwtTokenService.loadAuthentication(accessToken);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        if (isApiKey(accessToken)) {
            return remoteTokenServices.readAccessToken(accessToken);
        }
        return jwtTokenService.readAccessToken(accessToken);
    }

    private static boolean isApiKey(String value) {
        if (Strings.isNullOrEmpty(value)) {
            return false;
        }
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
