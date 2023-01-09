package bio.overture.score.server.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Slf4j
@Getter
@Setter
@Component
@Validated
@Profile("secure")
@ConfigurationProperties("auth.server.scope")
public class ScopeProperties {

  private final ScopePermissionProperties upload = new ScopePermissionProperties();
  private final ScopePermissionProperties download = new ScopePermissionProperties();

  @Getter
  @Setter
  @NoArgsConstructor
  public static class  ScopePermissionProperties{
    @NotBlank private String system;
    private final StudyPermissionProperties study = new StudyPermissionProperties();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class StudyPermissionProperties {
      @NotBlank private String prefix;
      @NotBlank private String suffix;
    }

  }

  public void logScopeProperties(){
    log.info("Using uploadSystemScope: {}",upload.getSystem());
    log.info("Using uploadStudyScopePrefix: {}",upload.getStudy().getPrefix());
    log.info("Using uploadStudyScopeSuffix: {}",upload.getStudy().getSuffix());
    log.info("Using downloadSystemScope: {}",download.getSystem());
    log.info("Using downloadStudyScopePrefix: {}",download.getStudy().getPrefix());
    log.info("Using downloadStudyScopeSuffix: {}",download.getStudy().getSuffix());
  }

}
