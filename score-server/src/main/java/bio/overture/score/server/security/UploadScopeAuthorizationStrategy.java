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
package bio.overture.score.server.security;

import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.metadata.MetadataService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class UploadScopeAuthorizationStrategy extends AbstractScopeAuthorizationStrategy {
  MetadataService metadataService;

  public UploadScopeAuthorizationStrategy(String scope, MetadataService metadataService) {
    super(AuthScope.from(scope));
    this.metadataService = metadataService;
  }

  @Override
  protected boolean verify(@NonNull List<AuthScope> grantedScopes, @NonNull final String objectId) {
    if (grantedScopes.stream().anyMatch(AuthScope::allowAllProjects)) {
      log.info("Access granted to blanket scope");
      return true;
    }

    val projectCodes = getAuthorizedProjectCodes(grantedScopes);
    val projCd = fetchProjectCode(objectId);
    val result = projectCodes.contains(projCd);
    log.info("checking for permission to project {} for object id {} ({})", projCd, objectId, result);

    return result;
  }

  protected List<String> getAuthorizedProjectCodes(@NonNull List<AuthScope> grantedScopes) {
    return getScope().matchingProjects(grantedScopes);
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
