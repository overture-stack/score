package bio.overture.score.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.*;

@Configuration
public class ProfileConfig {

  @Autowired
  Environment environment;

  @Bean
  String activeStorageProfile() {
    Map<String, String> storageProfiles = Map.of("collaboratory", "s3", "azure", "az", "test", "test");
    HashSet<String> activeStorageProfiles = new HashSet(Set.of(environment.getActiveProfiles()));
    activeStorageProfiles.retainAll(storageProfiles.keySet());
    return storageProfiles.get(activeStorageProfiles.stream().findFirst().get());
  }

}
