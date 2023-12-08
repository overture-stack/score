package bio.overture.score.server.config;

import bio.overture.score.core.model.StorageProfiles;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestProfileConfig {

  @Bean
  String activeStorageProfile() {
    return StorageProfiles.AZURE.name();
  }
}
