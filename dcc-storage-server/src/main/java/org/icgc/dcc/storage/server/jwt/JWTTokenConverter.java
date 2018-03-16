package org.icgc.dcc.storage.server.jwt;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.storage.server.util.TypeUtils;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.Map;

@Slf4j
public class JWTTokenConverter extends JwtAccessTokenConverter {

    public JWTTokenConverter(String publicKey) {
        super();
        this.setVerifierKey(publicKey);
    }

    @Override
    public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
        OAuth2Authentication authentication = super.extractAuthentication(map);

        val context = (Map<String, ?>)map.get("context");
        val user = (Map<String, ?>)context.get("user");
        val jwtUser = TypeUtils.convertType(user, JWTUser.class);

        authentication.setDetails(jwtUser);

        return authentication;
    }
}
