package bio.overture.score.server.config;

import bio.overture.score.server.security.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
@Profile("secure")
public class TokenServicesConfig {

    @Value("${auth.server.url}") private String checkTokenUrl;
    @Value("${auth.server.tokenName:token}") private String tokenName;
    @Value("${auth.server.clientId}") private String clientId;
    @Value("${auth.server.clientSecret}") private String clientSecret;

    @Bean
    @Profile("!jwt")
    public RemoteTokenServices remoteTokenServices() {
        return createRemoteTokenServices();
    }

    @Bean
    @Autowired
    @Profile("jwt")
    public MergedServerTokenServices mergedServerTokenServices(
            @NonNull PublicKeyFetcher publicKeyFetcher,
            @NonNull RetryTemplate retryTemplate
    ) {
        val jwtTokenServices = createJwtTokenServices(publicKeyFetcher.getPublicKey());
        val remoteTokenServices = createRemoteTokenServices();
        return new MergedServerTokenServices(jwtTokenServices, remoteTokenServices, retryTemplate);
    }

    @Bean
    @Autowired
    @Profile("jwt")
    public PublicKeyFetcher publicKeyFetcher(
            @Value("${auth.jwt.publicKeyUrl}") @NonNull String publicKeyUrl,
            @NonNull RetryTemplate retryTemplate) {
        return new DefaultPublicKeyFetcher(publicKeyUrl, new RestTemplate(), retryTemplate);
    }

    private AccessTokenConverter accessTokenConverter() {
        return new AccessTokenConverterWithExpiry();
    }

    private RemoteTokenServices createRemoteTokenServices() {
        val remoteTokenServices = new CachingRemoteTokenServices();
        remoteTokenServices.setCheckTokenEndpointUrl(checkTokenUrl);
        remoteTokenServices.setClientId(clientId);
        remoteTokenServices.setClientSecret(clientSecret);
        remoteTokenServices.setTokenName(tokenName);
        remoteTokenServices.setAccessTokenConverter(accessTokenConverter());

        log.debug("using auth server: " + checkTokenUrl);

        return remoteTokenServices;
    }

    private DefaultTokenServices createJwtTokenServices(String publicKey) {
        val tokenStore = new JwtTokenStore(new JWTConverter(publicKey));
        val defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore);
        return defaultTokenServices;
    }

}
