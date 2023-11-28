package bio.overture.score.client.config;

import bio.overture.score.client.exception.NotRetryableException;
import bio.overture.score.core.model.StorageProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;

@TestConfiguration
public class TestProfileConfig {

    @Autowired
    @Value("${defaultProfile:collaboratory}")
    private String defaultProfile;

    @Bean
    public String storageProfile(){
        return StorageProfiles.getProfileValue(defaultProfile);
    }

}
