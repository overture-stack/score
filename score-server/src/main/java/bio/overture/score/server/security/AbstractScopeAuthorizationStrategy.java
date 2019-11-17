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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

import java.util.Collections;
import java.util.List;

@Slf4j
@Getter
@AllArgsConstructor
public abstract class AbstractScopeAuthorizationStrategy {
  private AuthScope scope;
  protected abstract boolean verify(@NonNull List<AuthScope> grantedScopes, @NonNull final String objectId);

  /**
   * Main entry point: version of authorize() method that assumes object id is passed-in (method-level security has
   * access to this)
   */
  public boolean authorize(@NonNull Authentication authentication, @NonNull final String objectId) {
    log.info("Checking authorization for operation with scope {} on object with id {} ", scope, objectId);
    List<AuthScope> applicableScopes;

    if (authentication instanceof OAuth2Authentication) {
      val scopes =((OAuth2Authentication) authentication).getOAuth2Request().getScope();
      applicableScopes = scope.matchingScopes(scopes);
      log.info("Found authorized scopes '{}'", applicableScopes);
    } else {
      log.warn("Unknown authentication type: no authorized scopes available.");
      applicableScopes = Collections.emptyList();
    }

    val status = verify(applicableScopes, objectId);
    log.info("Authorization was {}", status? "granted":"denied");
    return status;
  }

}