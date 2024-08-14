package bio.overture.score.client.config;

import bio.overture.score.client.download.DownloadStateStore;
import bio.overture.score.core.util.SimplePartCalculator;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class StorageConfig {

  @Value("${storage.url}")
  private String endpoint;

  @Value("${download.partSize:1048576}")
  private int partSize;

  @Autowired private DownloadStateStore downloadStateStore;

  @Autowired
  @Qualifier("dataTemplate")
  private RestTemplate dataTemplate;

  @Autowired RetryTemplate retry;

  @Bean
  public SimplePartCalculator partCalculator() {
    return new SimplePartCalculator(partSize);
  }
}
