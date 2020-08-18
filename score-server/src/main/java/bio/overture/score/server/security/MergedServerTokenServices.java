package bio.overture.score.server.security;

import lombok.RequiredArgsConstructor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.util.UUID;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiredArgsConstructor
public class MergedServerTokenServices implements ResourceServerTokenServices {
    private final DefaultTokenServices jwtTokenService;
    private final RemoteTokenServices remoteTokenServices;
    private final RetryTemplate retryTemplate;

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        if (isApiKey(accessToken)) {
            return retryTemplate.execute(x -> remoteTokenServices.loadAuthentication(accessToken));
        }
        return jwtTokenService.loadAuthentication(accessToken);
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        if (isApiKey(accessToken)) {
            return retryTemplate.execute(x ->remoteTokenServices.readAccessToken(accessToken));
        }
        return jwtTokenService.readAccessToken(accessToken);
    }

    private static boolean isApiKey(String value) {
        if (isNullOrEmpty(value)) {
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
