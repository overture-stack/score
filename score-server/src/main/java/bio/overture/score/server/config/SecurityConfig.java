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
import bio.overture.score.server.security.scope.DownloadScopeAuthorizationStrategy;
import bio.overture.score.server.security.scope.UploadScopeAuthorizationStrategy;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.provider.authentication.BearerTokenExtractor;
import org.springframework.security.oauth2.provider.authentication.TokenExtractor;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Resource service configuration file.<br>
 * Protects resources with access token obtained at the authorization server.
 */

@Slf4j
@Configuration
@Profile("secure")
@EnableWebSecurity
@EnableResourceServer
public class SecurityConfig extends ResourceServerConfigurerAdapter {

  private TokenExtractor tokenExtractor = new BearerTokenExtractor();

  private final ScopeProperties scopeProperties;

  @Autowired
  public SecurityConfig(@NonNull ScopeProperties scopeProperties) {
    this.scopeProperties = scopeProperties;
  }

  @Override
  public void configure(@NonNull HttpSecurity http) throws Exception {
    http.addFilterAfter(new OncePerRequestFilter() {

      @Override

      protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain)
        throws ServletException, IOException {

        // We don't want to allow access to a resource with no token so clear
        // the security context in case it is actually an OAuth2Authentication
        if (tokenExtractor.extract(request) == null) {
          SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
      }

    }, AbstractPreAuthenticatedProcessingFilter.class);

    http.csrf().disable();
    configureAuthorization(http);
  }

  private void configureAuthorization(HttpSecurity http) throws Exception {
    scopeProperties.logScopeProperties();;

    // @formatter:off     
    http
      .authorizeRequests()
      .antMatchers("/health").permitAll()
      .antMatchers("/upload/**").permitAll()
      .antMatchers("/download/**").permitAll()
      .and()
      
      .authorizeRequests()
      .anyRequest().authenticated();
    // @formatter:on
    log.info("initialization done");
  }

  @Bean
  public UploadScopeAuthorizationStrategy projectSecurity(@Autowired MetadataService song) {
    return new UploadScopeAuthorizationStrategy(
        scopeProperties.getUpload().getStudy().getPrefix(),
        scopeProperties.getUpload().getStudy().getSuffix(),
        scopeProperties.getUpload().getSystem(),
         song);
  }

  @Bean
  @Scope("prototype")
  public DownloadScopeAuthorizationStrategy accessSecurity(@Autowired MetadataService song) {
    return new DownloadScopeAuthorizationStrategy(
        scopeProperties.getDownload().getStudy().getPrefix(),
        scopeProperties.getDownload().getStudy().getSuffix(),
        scopeProperties.getDownload().getSystem(),
        song);
  }

  public ScopeProperties getScopeProperties() { return this.scopeProperties; }

}
