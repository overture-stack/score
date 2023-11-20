package bio.overture.score.client.config;

import bio.overture.score.client.exception.NotRetryableException;
import bio.overture.score.client.storage.StorageService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class ProfileConfig {

  @Autowired
  private RestTemplate serviceTemplate;

  @Value("${storage.url}")
  @NonNull
  private String endpoint;

  @Autowired
  @Value("${isTest}")
  private boolean isTest = false;

  @Qualifier("clientVersion")
  @Autowired
  @NonNull
  String clientVersion;

  @Bean
  public String storageProfile(){
    String profile =  getStorageProfile();
    return profile;
  }

  private String getStorageProfile() {
    if(isTest){ return ""; }
    log.debug("get profile endpoint: "+endpoint);
  try{
    String storageProfile = serviceTemplate.exchange(endpoint + "/profile", HttpMethod.GET, defaultEntity(), String.class).getBody();
    return storageProfile;
  }catch(NotRetryableException nre ){
    log.error("received exception when getting profiles: " + nre.getMessage());
  }
  return "collaboratory";
  }


  private HttpEntity<Object> defaultEntity() {
    return new HttpEntity<Object>(defaultHeaders());
  }

  private HttpHeaders defaultHeaders() {
    val requestHeaders = new HttpHeaders();
    requestHeaders.add(HttpHeaders.USER_AGENT, clientVersion);
    return requestHeaders;
  }

}
