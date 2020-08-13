package bio.overture.score.server.security;


import bio.overture.score.server.utils.JWTGenerator;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

import java.security.KeyPairGenerator;
import java.util.List;
import java.util.UUID;

import static bio.overture.score.server.utils.JwtContext.buildJwtContext;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MergedServerTokenServicesTest {
    private static final String API_KEY = UUID.randomUUID().toString();

    private MergedServerTokenServices mergedServerTokenServices;
    @Mock private RemoteTokenServices remoteTokenServices;
    @Mock private DefaultTokenServices jwtTokenServices;


    private JWTGenerator jwtGenerator;

    @Before
    @SneakyThrows
    public void beforeTest() {
        val keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(1024);
        jwtGenerator = new JWTGenerator(keyGenerator.generateKeyPair());
        mergedServerTokenServices = new MergedServerTokenServices(jwtTokenServices, remoteTokenServices);
    }

    @Test
    public void accessTokenResolution_apiKey_success() {
        when(remoteTokenServices.loadAuthentication(API_KEY)).thenReturn(null);
        when(remoteTokenServices.readAccessToken(API_KEY)).thenReturn(null);
        mergedServerTokenServices.loadAuthentication(API_KEY);
        mergedServerTokenServices.readAccessToken(API_KEY);
        verify(remoteTokenServices, times(1)).loadAuthentication(API_KEY);
        verify(remoteTokenServices, times(1)).readAccessToken(API_KEY);
    }

    @Test
    public void accessTokenResolution_jwt_success() {
        val jwtString = jwtGenerator.generateJwtWithContext(buildJwtContext(List.of("score.WRITE")), false);
        when(jwtTokenServices.loadAuthentication(jwtString)).thenReturn(null);
        when(jwtTokenServices.readAccessToken(jwtString)).thenReturn(null);
        mergedServerTokenServices.loadAuthentication(jwtString);
        mergedServerTokenServices.readAccessToken(jwtString);
        verify(jwtTokenServices, times(1)).loadAuthentication(jwtString);
        verify(jwtTokenServices, times(1)).readAccessToken(jwtString);
    }
}
