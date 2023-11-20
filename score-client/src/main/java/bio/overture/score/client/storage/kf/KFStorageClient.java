package bio.overture.score.client.storage.kf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_LENGTH;

@Builder
@Component
@Profile("kf")
public class KFStorageClient {

  private static final long MIN_EXPIRATION = 3600;
  private final String jwt;
  private final String tokenUrl;
  private final String apiUrl;
  private final long urlExpiration;
  private final RetryTemplate retry;
  private final RestTemplate restTemplate = new RestTemplate();

  public KFStorageClient(
      @Value("${client.accessToken}") @NonNull String jwt,
      @Value("${kf.token.url}") @NonNull String tokenUrl,
      @Value("${kf.download.url}") @NonNull String apiUrl,
      @Value("${kf.download.expiration}") long urlExpiration,
      @Autowired RetryTemplate retry ){
    this.jwt = jwt;
    this.apiUrl = apiUrl;
    this.urlExpiration = Math.max(urlExpiration, MIN_EXPIRATION);
    this.tokenUrl = tokenUrl;
    this.retry = retry;
  }

  public String generateAccessToken(){
    val accessTokenResponse = getObject(AccessTokenResponse.class, jwt, tokenUrl);
    return accessTokenResponse.getAccessToken();
  }

  public PresignedUrl generatePresignedUrl(@NonNull String objectId){
    val accessToken = generateAccessToken();
    val response = getResponse(UrlResponse.class, accessToken, getKFDownloadEndpoint(objectId));
    val url = response.getBody().getUrl();
    val size = peekResponseContentSize(url);
    return PresignedUrl.builder()
        .size(size)
        .url(url)
        .build();
  }

  private String getKFDownloadEndpoint(String objectId){
    return format("%s/user/data/download/%s?expires_in=%s", apiUrl, objectId, urlExpiration);
  }

  @SneakyThrows
  private <T> ResponseEntity<T> getResponse(Class<T> responseType, String accessToken, String url){
    //val entity = new HttpEntity<T>(null, buildAuthHeader(accessToken));
    HttpEntity entity = new HttpEntity<T>(null, buildAuthHeader(accessToken));
    return retry.execute(ctx -> restTemplate.exchange(new URI(url), HttpMethod.GET, entity, responseType));
  }

  @SneakyThrows
  private <T> T getObject(Class<T> responseType, String accessToken, String url){
    return getResponse(responseType, accessToken, url).getBody();
  }

  @SneakyThrows
  private static long peekResponseContentSize(String url){
    val u = new URL(url);
    val conn = (HttpURLConnection)u.openConnection();
    val contentLength = conn.getContentLengthLong();
    checkState(contentLength != -1,
        "The following response does not contain the '%s' header: %s",
        CONTENT_LENGTH, conn.getHeaderFields());
    checkState(contentLength > 0,
        "The following response contains a content length of 0: %s", conn.getHeaderFields());
    return contentLength;
  }

  private static HttpHeaders buildAuthHeader(String accessToken ){
    val headers = new HttpHeaders();
    if (!isNull(accessToken)){
      headers.set(AUTHORIZATION, "Bearer "+accessToken);
    }
    return headers;
  }

  @lombok.Value
  @Builder
  public static class PresignedUrl {
    @NonNull private final String url;
    private final long size;
  }

  @Data
  public static class AccessTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;
  }

  @Data
  public static class UrlResponse{
    private String url;
  }

}
