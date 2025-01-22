package bio.overture.score.server.config;

import bio.overture.score.server.metadata.MetadataService;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;

@Profile("noSecurityDev")
@Configuration
@Getter
@Setter
@EnableWebSecurity
public class NoSecurityConfig extends WebSecurityConfigurerAdapter {

  private String provider;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // Disable cors and csrf to avoid 403 forbidden for local development.
    http.cors().and().csrf().disable().authorizeRequests().antMatchers("/").permitAll();
  }

  /*
   * In order to mirror the security provisions in SecurityConfig.java: we need to define projectSecurity, and
   * accessSecurity, which are used for method level authorization in the DownloadController and UploadController. These
   * need to return a ScopeAuthorizationStrategy, which we do here by returning an anonymous class version of the
   * expected return class, and these anonymous implementations override the authorize() function call to always return
   * true.
   */
  public interface AnonymousScopeAuthorizationStategy {

    boolean authorize(@NonNull Authentication authentication, @NonNull String objectId);
  }

  @Bean
  public AnonymousScopeAuthorizationStategy projectSecurity(
      @Autowired MetadataService metadataService) {
    return new AnonymousScopeAuthorizationStategy() {

      @Override
      public boolean authorize(@NonNull Authentication authentication, @NonNull String objectId) {
        return true;
      }
    };
  }

  @Bean
  @Scope("prototype")
  public AnonymousScopeAuthorizationStategy accessSecurity(
      @Autowired MetadataService metadataService) {
    return new AnonymousScopeAuthorizationStategy() {

      @Override
      public boolean authorize(@NonNull Authentication authentication, @NonNull String objectId) {
        return true;
      }
    };
  }
}
