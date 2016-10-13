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
package org.icgc.dcc.storage.server.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class ProjectScopeStrategy extends AbstractScopeAuthorizationStrategy {

  @Value("${auth.server.uploadScope}")
  protected String uploadScope;

  public ProjectScopeStrategy() {
    super();
  }

  ProjectScopeStrategy(final String scopeStr) {
    super(scopeStr);
  }

  @Override
  protected void setAuthorizeScope(String scopeStr) {
    uploadScope = scopeStr;
  }

  @Override
  protected String getAuthorizeScope() {
    return uploadScope;
  }

  @Override
  protected boolean verify(@NonNull List<AuthScope> grantedScopes, @NonNull final String objectId) {
    return verifyProjectAccess(grantedScopes, objectId);
  }

  protected boolean verifyProjectAccess(@NonNull List<AuthScope> grantedScopes, @NonNull final String objectId) {
    val projectCodes = getAuthorizedProjectCodes(grantedScopes);

    boolean result = false;
    if (projectCodes.contains(AuthScope.ALL_PROJECTS)) {
      log.info("Access granted to blanket scope");
      result = true;
    } else {
      val projCd = fetchProjectCode(objectId);
      result = projectCodes.contains(projCd);
      log.info("checking for permission to project {} for object id {} ({})", projCd, objectId, result);
    }
    return result;
    // return projectCodes.contains(AuthScope.ALL_PROJECTS) ? true : projectCodes.contains(fetchProjectCode(objectId));
  }

  protected List<String> getAuthorizedProjectCodes(@NonNull List<AuthScope> grantedScopes) {
    return extractProjects(scope, grantedScopes);
  }

  /**
   * Extracts project codes (strings) from list of AuthScopes. This method broken out from getAuthorizedProjectCodes()
   * to isolate actual mapping logic from organizing inputs; facilitating unit testing.
   * 
   * @param uploadScope - the expected project/operation to evaluate scope for i.e., collab.upload, aws.upload
   * @param scopes - list of scopes following convention of the form {system}.{project-code}.{operation} i.e.,
   * collab.BRCA-US.upload
   * @return list of project codes
   */
  protected List<String> extractProjects(@NonNull final AuthScope uploadScope,
      @NonNull final Collection<AuthScope> scopes) {
    val result = scopes.stream().filter(s -> s.matches(uploadScope))
        .map(s -> s.getProject())
        .collect(Collectors.toList());

    return result;
  }

  /**
   * Retrieve project code from Metadata Service for specific object id
   * @param objectId
   * @return project code
   */
  protected String fetchProjectCode(@NonNull final String objectId) {
    // makes a query to meta service to retrieve project code for the given object id
    val entity = metadataService.getEntity(objectId);
    if (entity != null) {
      return entity.getProjectCode();
    } else {
      val msg = String.format("Failed to retrieve metadata for objectId: %s", objectId);
      log.error(msg);
      throw new NotRetryableException(new IllegalArgumentException(msg));
    }
  }

}
