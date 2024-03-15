/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package bio.overture.score.server.config;

import bio.overture.score.server.metadata.MetadataService;
import bio.overture.score.server.properties.ScopeProperties;
import bio.overture.score.server.security.ApiKeyIntrospector;
import bio.overture.score.server.security.scope.DownloadScopeAuthorizationStrategy;
import bio.overture.score.server.security.scope.UploadScopeAuthorizationStrategy;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

/**
 * Resource service configuration file.<br>
 * Protects resources with access token obtained at the authorization server.
 */
@Slf4j
@Configuration
@Profile("secure")
@EnableWebSecurity
@Getter
@Setter
@ConfigurationProperties("auth.server")
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private String url;
  private String clientId;
  private String clientSecret;
  private String provider;
  private String tokenName;

  private final ScopeProperties scopeProperties;

  @Autowired private JwtDecoder jwtDecoder;

  @Autowired
  public SecurityConfig(@NonNull ScopeProperties scopeProperties) {
    this.scopeProperties = scopeProperties;
  }

  @Override
  public void configure(@NonNull HttpSecurity http) throws Exception {
    http.csrf().disable();
    configureAuthorization(http);
  }

  private void configureAuthorization(HttpSecurity http) throws Exception {
    scopeProperties.logScopeProperties();

    // @formatter:off
    http.authorizeRequests()
        .antMatchers("/health")
        .permitAll()
        .antMatchers("/actuator/health")
        .permitAll()
        .antMatchers("/upload/**")
        .permitAll()
        .antMatchers("/download/**")
        .permitAll()
        .antMatchers("/swagger**", "/swagger-resources/**", "/v2/api**", "/webjars/**")
        .permitAll()
        .and()
        .authorizeRequests()
        .anyRequest()
        .authenticated();

    http.oauth2ResourceServer(
        oauth2 -> oauth2.authenticationManagerResolver(this.tokenAuthenticationManagerResolver()));
    // @formatter:on
    log.info("initialization done");
  }

  @Bean
  public AuthenticationManagerResolver<HttpServletRequest> tokenAuthenticationManagerResolver() {

    // Auth Managers for JWT and for ApiKeys. JWT uses the default auth provider,
    // but OpaqueTokens are handled by the custom ApiKeyIntrospector
    AuthenticationManager jwt = new ProviderManager(new JwtAuthenticationProvider(jwtDecoder));
    AuthenticationManager opaqueToken =
        new ProviderManager(
            new OpaqueTokenAuthenticationProvider(
                new ApiKeyIntrospector(url, clientId, clientSecret, tokenName)));

    return (request) -> useJwt(request) ? jwt : opaqueToken;
  }

  @Bean
  public UploadScopeAuthorizationStrategy projectSecurity(@Autowired MetadataService song) {
    return new UploadScopeAuthorizationStrategy(
        scopeProperties.getUpload().getStudy().getPrefix(),
        scopeProperties.getUpload().getStudy().getSuffix(),
        scopeProperties.getUpload().getSystem(),
        song,
        provider);
  }

  @Bean
  @Scope("prototype")
  public DownloadScopeAuthorizationStrategy accessSecurity(@Autowired MetadataService song) {
    return new DownloadScopeAuthorizationStrategy(
        scopeProperties.getDownload().getStudy().getPrefix(),
        scopeProperties.getDownload().getStudy().getSuffix(),
        scopeProperties.getDownload().getSystem(),
        song,
        provider);
  }

  public ScopeProperties getScopeProperties() {
    return this.scopeProperties;
  }

  @Bean
  public OpaqueTokenIntrospector introspector() {
    return new ApiKeyIntrospector(url, clientId, clientSecret, tokenName);
  }

  private boolean useJwt(HttpServletRequest request) {
    val authorizationHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorizationHeaderValue != null && authorizationHeaderValue.startsWith("Bearer")) {
      String token = authorizationHeaderValue.substring(7);
      try {
        UUID.fromString(token);
        // able to parse as UUID, so this token matches our EgoApiKey format
        return false;
      } catch (IllegalArgumentException e) {
        // unable to parse as UUID, use our JWT resolvers
        return true;
      }
    }
    return true;
  }
}
