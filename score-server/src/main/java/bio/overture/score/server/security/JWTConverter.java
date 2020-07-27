package bio.overture.score.server.security;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.*;

@Slf4j
public class JWTConverter extends JwtAccessTokenConverter {

    private final static String CONTEXT = "context";
    private final static String SCOPE = "scope";

    @SneakyThrows
    public JWTConverter(String publicKey) {
        super();
        this.setVerifierKey(publicKey);
        this.afterPropertiesSet();
    }

    @Override
    public OAuth2Authentication extractAuthentication(@NonNull Map<String, ?> map) {
        // Currently EGO's JWT spec places scopes in map at 'context.scope'
        // but extractAuthentication expects them in map's root at 'scope'
        // so put all scopes into root level for spring security processing
        val allScopes = getRootAndContextScopes(map);
        HashMap<String, Object> updatedMap = new HashMap<>(map);
        updatedMap.put(SCOPE, allScopes);

        return super.extractAuthentication(updatedMap);
    }

    private Collection<String> getRootAndContextScopes(Map<String, ?> map) {
        List<String> extractedScopes = new ArrayList<>(Collections.emptyList());
        try {
            if (map.containsKey(CONTEXT)) {
                val context = (Map<String, Object>) map.get("context");
                extractedScopes.addAll((Collection<String>) context.get("scope"));
            }
            if (map.containsKey(SCOPE)) {
                extractedScopes.addAll((Collection<String>) map.get(SCOPE));
            }
        } catch (Exception e) {
            log.error("Failed to extract scopes from JWT");
        }
        return extractedScopes;
    }
}
