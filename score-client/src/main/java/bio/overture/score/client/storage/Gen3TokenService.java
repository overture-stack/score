package bio.overture.score.client.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Builder
public class Gen3TokenService {

  private String jwt;
  private String keyStoreUrl;
  private String apiUrl;

  public Gen3TokenService(
      @Value("${client.accessToken}") @NonNull String jwt,
    @Value("${gen3.keystoreUrl}") @NonNull String keyStoreUrl,
    @Value("gen3.apiUrl") @NonNull String apiUrl){
    this.jwt = jwt;
    this.apiUrl = apiUrl;
    this.keyStoreUrl = keyStoreUrl;
  }

  private final RestTemplate restTemplate = new RestTemplate();

  public String generateAccessToken(){
    val key = getGen3ApiKey();
    return getGen3AccessToken(key).getAccessToken();
  }

  private AccessToken getGen3AccessToken(Gen3ApiKey gen3ApiKey){
    return post(AccessToken.class, jwt, getGen3AccessTokenEndpoint(), gen3ApiKey);
  }

  private String getGen3AccessTokenEndpoint(){
    return String.format("%s/user/credentials/cdis/access_token", apiUrl);
  }

  private Gen3ApiKey getGen3ApiKey(){
    return get(Gen3ApiKey.class, jwt, keyStoreUrl);
  }

  @SneakyThrows
  private <T> T get(Class<T> responseType, String accessToken, String url){
    val entity = new HttpEntity<T>(null, buildAuthHeader(accessToken));
    return restTemplate.exchange(new URI(url), HttpMethod.GET, entity, responseType).getBody();
  }

  @SneakyThrows
  private <T, R> T post(Class<T> responseType, String accessToken, String url, R body ){
    val entity = new HttpEntity<R>(body, buildAuthHeader(accessToken));
    return restTemplate.exchange(new URI(url), HttpMethod.POST, entity, responseType).getBody();
  }

  private static HttpHeaders buildAuthHeader(String accessToken ){
    val headers = new HttpHeaders();
    if (!isNull(accessToken)){
      headers.set(AUTHORIZATION, "Bearer "+accessToken);
    }
    return headers;
  }

  @Data
  public static class Gen3ApiKey {
    @JsonProperty("key_id")
    private String keyId;

    @JsonProperty("api_key")
    private String apiKey;
  }

  @Data
  public static class AccessToken{
    @JsonProperty("access_token")
    private String accessToken;
  }

}
