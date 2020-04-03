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

import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.metadata.MetadataService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import static bio.overture.score.server.security.TokenChecker.isExpired;
import static bio.overture.score.server.util.Scopes.extractGrantedScopes;

@Slf4j
@Data
public class UploadScopeAuthorizationStrategy {
  @NonNull String studyPrefix;
  @NonNull String studySuffix;
  @NonNull String systemScope;
  @NonNull MetadataService metadataService;

  public boolean authorize(@NonNull Authentication authentication, @NonNull final String objectId) {
    if (isExpired(authentication)) {
      return false;
    }
    val grantedScopes = extractGrantedScopes(authentication);
    if (verifyOneOfSystemScope(grantedScopes)) {
      log.info("System-level authorization granted");
      return true;
    }
    log.info("Checking study-level authorization for objectId {}", objectId);
    return verifyOneOfStudyScope(grantedScopes, objectId);
  }

  public boolean verifyOneOfSystemScope(@NonNull Set<String> grantedScopes) {
    return grantedScopes.stream().anyMatch(s -> s.equalsIgnoreCase(systemScope));
  }
  public boolean verifyOneOfStudyScope(
      @NonNull Set<String> grantedScopes, @NonNull final String objectId) {
    val studyScope = getStudyScope(fetchStudyId(objectId));
    return grantedScopes.stream().anyMatch(studyScope::equalsIgnoreCase);
  }

  public String getStudyScope(@NonNull String studyId) {
    return studyPrefix + studyId + studySuffix;
  }

  /**
   * Retrieve project code from Metadata Service for specific object id
   *
   * @param objectId The id of the file that we want to upload/download.
   * @return The id of the study that the file part of.
   */
  protected String fetchStudyId(@NonNull final String objectId) {
    // makes a query to meta service to retrieve project code for the given object id
    val entity = metadataService.getEntity(objectId);
    if (entity != null) {
      val studyId = entity.getProjectCode();
      log.info("Fetched studyId '{}' for objectId '{}'", studyId, objectId);
      return studyId;
    } else {
      val msg = String.format("Failed to retrieve metadata for objectId: %s", objectId);
      log.error(msg);
      throw new NotRetryableException(new IllegalArgumentException(msg));
    }
  }

  /**
   * Retrieve access type for the given file
   *
   * @param objectId The id of the file to get the access type for.
   * @return The access type of the file.
   */
  public String fetchFileAccessType(@NonNull final String objectId) {
    // makes a query to meta service to retrieve project code for the given object id
    val entity = metadataService.getEntity(objectId);
    if (entity != null) {
      return entity.getAccess();
    } else {
      val msg = String.format("Failed to retrieve metadata for objectId: %s", objectId);
      log.error(msg);
      throw new NotRetryableException(new IllegalArgumentException(msg));
    }
  }
}
