package bio.overture.score.server.config;

import bio.overture.score.server.security.AccessTokenConverterWithExpiry;
import bio.overture.score.server.security.CachingRemoteTokenServices;
import bio.overture.score.server.security.JWTTokenConverter;
import bio.overture.score.server.security.MergedServerTokenServices;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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

    @Bean
    private AccessTokenConverter accessTokenConverter() {
        return new AccessTokenConverterWithExpiry();
    }
    @Bean
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

    @SneakyThrows
    private String fetchJWTPublicKey(String publicKeyUrl) {
        val restTemplate = new RestTemplate();
        val response = restTemplate.getForEntity(publicKeyUrl, String.class);
        return response.getBody();
    }
    @SneakyThrows
    private DefaultTokenServices createJwtTokenServices(String publicKeyUrl) {
        val publicKey = fetchJWTPublicKey(publicKeyUrl);
        val tokenStore = new JwtTokenStore(new JWTTokenConverter(publicKey));
        val defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore);
        return defaultTokenServices;
    }
    @Bean
    @Profile("jwt")
    public MergedServerTokenServices mergedServerTokenServices(
            @Value("${auth.jwt.publicKeyUrl}") @NonNull String publicKeyUrl)
    {
        val jwtTokenServices = createJwtTokenServices(publicKeyUrl);
        val remoteTokenServices = createRemoteTokenServices();
        return new MergedServerTokenServices(jwtTokenServices, remoteTokenServices);
    }
}
