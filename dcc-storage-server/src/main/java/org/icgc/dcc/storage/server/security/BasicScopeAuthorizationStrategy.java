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

import java.util.List;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class BasicScopeAuthorizationStrategy extends AbstractScopeAuthorizationStrategy {

  @Value("${auth.server.downloadScope}")
  private String downloadScope;

  public BasicScopeAuthorizationStrategy() {
    super();
  }

  BasicScopeAuthorizationStrategy(final String scopeStr) {
    super(scopeStr);
  }

  @Override
  protected void setAuthorizeScope(String scopeStr) {
    downloadScope = scopeStr;
  }

  @Override
  protected String getAuthorizeScope() {
    return downloadScope;
  }

  @Override
  protected boolean verify(@NonNull List<AuthScope> grantedScopes, @NonNull final String objectId) {
    return verifyAccessType(grantedScopes, objectId);
  }

  protected boolean verifyAccessType(@NonNull List<AuthScope> grantedScopes, @NonNull final String objectId) {
    val objectAccessType = fetchObjectAccessType(objectId);
    val accessType = new Access(objectAccessType);

    if (accessType.isOpen()) {
      return true;
    } else if (accessType.isControlled()) {
      return verifyBasicScope(grantedScopes);
    } else {
      val msg =
          String.format("Invalid access type '%s' found in Metadata record for object id: %s", objectAccessType,
              objectId);
      log.error(msg);
      throw new NotRetryableException(new IllegalArgumentException(msg));
    }
  }

  protected boolean verifyBasicScope(@NonNull List<AuthScope> grantedScopes) {
    boolean result = false;
    val check = grantedScopes.stream().filter(s -> s.matches(scope)).collect(Collectors.toList());
    result = !check.isEmpty();
    return result;
  }

  /**
   * Retrieve project code from Metadata Service for specific object id
   * @param objectId
   * @return project code
   */
  protected String fetchObjectAccessType(@NonNull final String objectId) {
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
