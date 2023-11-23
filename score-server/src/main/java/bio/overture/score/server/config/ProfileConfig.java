package bio.overture.score.server.config;

import bio.overture.score.core.model.StorageProfiles;
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
    HashSet<String> activeStorageProfiles = new HashSet(Set.of(environment.getActiveProfiles()));
    activeStorageProfiles.retainAll(StorageProfiles.keySet());
    return StorageProfiles.getProfileValue(activeStorageProfiles.stream().findFirst().get());
  }

}
