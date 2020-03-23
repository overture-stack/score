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
package bio.overture.score.server.security;

import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.Authentication;

import static bio.overture.score.server.security.TokenChecker.isExpired;
import static bio.overture.score.server.util.Scopes.extractGrantedScopes;

@Slf4j
@Value
@Builder
public class StudySecurity {
  @NonNull String studyPrefix;
  @NonNull String studySuffix;
  @NonNull String systemScope;

  public boolean authorize(@NonNull Authentication authentication, @NonNull final String studyId) {
    if (isExpired(authentication)) {
      return false;
    }
    log.info("Checking study-level authorization for studyId {}", studyId);
    val grantedScopes = extractGrantedScopes(authentication);
    return verifyOneOfStudyScope(grantedScopes, studyId);
  }

  public boolean isGrantedForStudy(@NonNull String tokenScope, @NonNull String studyId) {
    log.info(
        "Checking if input scope '{}' is granted for study scope '{}'",
        tokenScope,
        getStudyScope(studyId));
    return systemScope.equals(tokenScope) || isScopeMatchStudy(tokenScope, studyId); // short-circuit
  }

  public boolean verifyOneOfStudyScope(
      @NonNull Set<String> grantedScopes, @NonNull final String studyId) {
    return grantedScopes.stream().anyMatch(s -> isGrantedForStudy(s, studyId));
  }

  public boolean isScopeMatchStudy(@NonNull String tokenScope, @NonNull String studyId) {
    return getStudyScope(studyId).equals(tokenScope);
  }

  public String getStudyScope(@NonNull String studyId) {
    return studyPrefix + studyId + studySuffix;
  }
}
