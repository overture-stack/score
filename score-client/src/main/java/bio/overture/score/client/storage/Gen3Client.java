package bio.overture.score.client.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;

@Builder
public class Gen3Client {

  private final String jwt;
  private final String keyStoreUrl;
  private final String apiUrl;
  private final RetryTemplate retry;

  public Gen3Client(
      @Value("${client.accessToken}") @NonNull String jwt,
    @Value("${gen3.keystoreUrl}") @NonNull String keyStoreUrl,
    @Value("gen3.apiUrl") @NonNull String apiUrl,
      @Autowired RetryTemplate retry ){
    this.jwt = jwt;
    this.apiUrl = apiUrl;
    this.keyStoreUrl = keyStoreUrl;
    this.retry = retry;
  }

  private final RestTemplate restTemplate = new RestTemplate();

  public String generateAccessToken(){
    val key = getGen3ApiKey();
    return getGen3AccessToken(key).getAccessToken();
  }

  public PresignedUrl generatePresignedUrl(@NonNull String objectId){
    val accessToken = generateAccessToken();
    val response = getResponse(UrlResponse.class, accessToken, getGen3DownloadEndpoint(objectId));
    val url = response.getBody().getUrl();
    val size = parseContentLength(response);
    return PresignedUrl.builder()
        .size(size)
        .url(url)
        .build();
  }


  private static long parseContentLength(ResponseEntity<?> response){
    val contentLength = response.getHeaders().getContentLength();
    checkState(contentLength != -1,
        "The following response does not contain the '%s' header: %s",
        CONTENT_LENGTH, response );
    checkState(contentLength > 0,
        "The following response contains a content length of 0: %s", response);
    return contentLength;
  }

  private AccessTokenResponse getGen3AccessToken(Gen3ApiKeyResponse gen3ApiKeyResponse){
    return post(AccessTokenResponse.class, jwt, getGen3AccessTokenEndpoint(), gen3ApiKeyResponse);
  }

  //`curl -XGET -H "Authorization: Bearer $ACCESS_TOKEN" "$GEN3_API_ROOT/user/data/download/$LATEST_DID"`
  private String getGen3DownloadEndpoint(String objectId){
    return String.format("%s/user/data/download/%s", apiUrl, objectId );
  }

  private String getGen3AccessTokenEndpoint(){
    return String.format("%s/user/credentials/cdis/access_token", apiUrl);
  }

  private Gen3ApiKeyResponse getGen3ApiKey(){
    return getObject(Gen3ApiKeyResponse.class, jwt, keyStoreUrl);
  }

  @SneakyThrows
  private <T> ResponseEntity<T> getResponse(Class<T> responseType, String accessToken, String url){
    val entity = new HttpEntity<T>(null, buildAuthHeader(accessToken));
    return retry.execute(ctx -> restTemplate.exchange(new URI(url), HttpMethod.GET, entity, responseType));
  }

  @SneakyThrows
  private <T> T getObject(Class<T> responseType, String accessToken, String url){
    return getResponse(responseType, accessToken, url).getBody();
  }

  @SneakyThrows
  private <T, R> T post(Class<T> responseType, String accessToken, String url, R body ){
    val entity = new HttpEntity<R>(body, buildAuthHeader(accessToken));
    return retry.execute(ctx -> restTemplate.exchange(new URI(url), HttpMethod.POST, entity, responseType).getBody());
  }

  @lombok.Value
  @Builder
  public static class PresignedUrl {
    @NonNull private final String url;
    private final long size;
  }

  private static HttpHeaders buildAuthHeader(String accessToken ){
    val headers = new HttpHeaders();
    if (!isNull(accessToken)){
      headers.set(AUTHORIZATION, "Bearer "+accessToken);
    }
    return headers;
  }

  @Data
  public static class Gen3ApiKeyResponse {
    @JsonProperty("key_id")
    private String keyId;

    @JsonProperty("api_key")
    private String apiKey;
  }

  @Data
  public static class AccessTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
  }

  @Data
  public static class UrlResponse{
    private String url;
  }

}
