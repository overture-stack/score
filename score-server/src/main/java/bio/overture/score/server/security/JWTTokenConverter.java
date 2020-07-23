package bio.overture.score.server.security;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

@Slf4j
public class JWTTokenConverter extends JwtAccessTokenConverter {
    private final String publicKey;
    @SneakyThrows
    public JWTTokenConverter(String publicKey) {
        super();
        this.publicKey = publicKey;
        this.setVerifierKey(publicKey);
        this.afterPropertiesSet();
    }

    @Override
    public OAuth2Authentication extractAuthentication(@NonNull Map<String, ?> map) {
        OAuth2Authentication authentication = super.extractAuthentication(map);


        // default authentication extraction doesn't resolve scopes to our JWT spec
        // so do it manually here
        val context = (Map<String, Object>)  map.get("context");
        val scopes = (Collection<String>) context.get("scope");
        val scopesSet = Set.copyOf(scopes);

        val currentReq = authentication.getOAuth2Request();

        OAuth2Request scopedRequest =
                new OAuth2Request(currentReq.getRequestParameters(),
                        currentReq.getClientId(),
                        currentReq.getAuthorities(),
                        currentReq.isApproved(),
                        scopesSet,
                        currentReq.getResourceIds(),
                        currentReq.getRedirectUri(),
                        currentReq.getResponseTypes(),
                        currentReq.getExtensions()
                );


        OAuth2Authentication authenticationWithAuthorities = new OAuth2Authentication(scopedRequest, authentication.getUserAuthentication());
        return authenticationWithAuthorities;
    }
}
