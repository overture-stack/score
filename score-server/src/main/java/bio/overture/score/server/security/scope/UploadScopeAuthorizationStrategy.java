/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package bio.overture.score.server.security.scope;

import bio.overture.score.server.metadata.MetadataService;
import bio.overture.score.server.repository.auth.KeycloakAuthorizationService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Set;

import static bio.overture.score.server.util.Scopes.extractGrantedScopes;
import static bio.overture.score.server.util.Scopes.extractGrantedScopesFromRpt;

@Slf4j
public class UploadScopeAuthorizationStrategy extends AbstractScopeAuthorizationStrategy {

  @Autowired
  private KeycloakAuthorizationService keycloakAuthorizationService;

  public UploadScopeAuthorizationStrategy(@NonNull String studyPrefix, @NonNull String studySuffix,
      @NonNull String systemScope, @NonNull MetadataService metadataService, @NonNull String provider){
    super(studyPrefix, studySuffix, systemScope, metadataService, provider);
  }

  public boolean authorize(@NonNull Authentication authentication, @NonNull final String objectId) {
    Set<String> grantedScopes;

    if("keycloak".equalsIgnoreCase(this.getProvider()) && authentication instanceof JwtAuthenticationToken) {
      val authGrants = keycloakAuthorizationService
          .fetchAuthorizationGrants(((JwtAuthenticationToken) authentication).getToken().getTokenValue());

      grantedScopes = extractGrantedScopesFromRpt(authGrants);
    } else {
      grantedScopes = extractGrantedScopes(authentication);
    }

    if (verifyOneOfSystemScope(grantedScopes)) {
      log.info("System-level upload authorization granted");
      return true;
    }
    log.info("Checking study-level authorization for objectId {}", objectId);
    return verifyOneOfStudyScope(grantedScopes, objectId);
  }

}
