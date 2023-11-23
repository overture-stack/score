package bio.overture.score.server.config;

import bio.overture.score.core.model.StorageProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashSet;
import java.util.Set;

@TestConfiguration
public class TestProfileConfig {

    @Bean
    String activeStorageProfile() {
        return StorageProfiles.AZURE.name();
    }
}
