package bio.overture.score.server.config;

import bio.overture.score.server.metadata.SongService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static bio.overture.score.server.metadata.SongService.createSongService;

@Configuration
public class SongServiceConfig {

  @Value("${metadata.url}")
  private String metadataUrl;

  @Bean
  public SongService songService(){
    return createSongService(metadataUrl);
  }

}
