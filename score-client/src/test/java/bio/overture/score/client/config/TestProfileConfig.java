package bio.overture.score.client.config;

import bio.overture.score.core.model.StorageProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestProfileConfig {

  @Autowired
  @Value("${defaultProfile:collaboratory}")
  private String defaultProfile;

  @Bean
  public String storageProfile() {
    return StorageProfiles.getProfileValue(defaultProfile);
  }
}
