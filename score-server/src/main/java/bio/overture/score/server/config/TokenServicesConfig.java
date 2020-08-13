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

    private @Value("${auth.server.url}") String checkTokenUrl;
    private @Value("${auth.server.tokenName:token}") String tokenName;
    private @Value("${auth.server.clientId}") String clientId;
    private @Value("${auth.server.clientSecret}") String clientSecret;

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
    @Bean
    @Profile("!jwt")
    public RemoteTokenServices remoteTokenServices() {
        return createRemoteTokenServices();
    }


    private DefaultTokenServices createJwtTokenServices(String publicKey) {
        val tokenStore = new JwtTokenStore(new JWTConverter(publicKey));
        val defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore);
        return defaultTokenServices;
    }
    @Bean
    @Autowired
    @Profile("jwt")
    public MergedServerTokenServices mergedServerTokenServices(
            @NonNull PublicKeyFetcher publicKeyFetcher
    ) {
        val jwtTokenServices = createJwtTokenServices(publicKeyFetcher.getPublicKey());
        val remoteTokenServices = createRemoteTokenServices();
        return new MergedServerTokenServices(jwtTokenServices, remoteTokenServices);
    }
    @Bean
    @Autowired
    @Profile("jwt")
    public PublicKeyFetcher publicKeyFetcher(
            @Value("${auth.jwt.publicKeyUrl}") @NonNull String publicKeyUrl,
            @NonNull RetryTemplate retryTemplate) {
        return new DefaultPublicKeyFetcher(publicKeyUrl, new RestTemplate(), retryTemplate);
    }
}
