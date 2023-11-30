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

import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.metadata.MetadataService;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.Authentication;

@Slf4j
@Getter
@RequiredArgsConstructor
public abstract class AbstractScopeAuthorizationStrategy {

  @NonNull private final String studyPrefix;
  @NonNull private final String studySuffix;
  @NonNull private final String systemScope;
  @NonNull private final MetadataService metadataService;

  public abstract boolean authorize(Authentication authentication, String objectId);

  protected boolean verifyOneOfSystemScope(@NonNull Set<String> grantedScopes) {
    return grantedScopes.stream().anyMatch(s -> s.equalsIgnoreCase(getSystemScope()));
  }

  protected boolean verifyOneOfStudyScope(
      @NonNull Set<String> grantedScopes, @NonNull final String objectId) {
    val studyScope = getStudyScope(fetchStudyId(objectId));
    return grantedScopes.stream().anyMatch(studyScope::equalsIgnoreCase);
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
  protected String fetchFileAccessType(@NonNull final String objectId) {
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

  private String getStudyScope(@NonNull String studyId) {
    return getStudyPrefix() + studyId + getStudySuffix();
  }
}
