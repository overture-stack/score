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
package bio.overture.score.server.security.scope;

import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.metadata.MetadataService;
import bio.overture.score.server.security.Access;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.Authentication;

import static bio.overture.score.server.security.TokenChecker.isExpired;
import static bio.overture.score.server.util.Scopes.extractGrantedScopes;

@Slf4j
public class DownloadScopeAuthorizationStrategy extends AbstractScopeAuthorizationStrategy {

  public DownloadScopeAuthorizationStrategy(@NonNull String studyPrefix, @NonNull String studySuffix,
    @NonNull String systemScope, MetadataService metadataService) {
    super(studyPrefix, studySuffix, systemScope, metadataService);
  }

  @Override
  public boolean authorize(Authentication authentication, @NonNull final String objectId) {
    if (isExpired(authentication)) {
      return false;
    }
    val grantedScopes = extractGrantedScopes(authentication);
    log.info("Checking system-level authorization for objectId {}", objectId);
    if (verifyOneOfSystemScope(grantedScopes)) {
      return true;
    }
    log.info("Checking access control level for objectId {}", objectId);
    val fileAccessType = fetchFileAccessType(objectId);
    val accessType = new Access(fileAccessType);
    if (accessType.isOpen()) {
      log.info("Access control level is open -- access granted");
      return true;
    } else if (accessType.isControlled()) {
      log.info("Access control level is controlled -- checking study level authorization.");
      return verifyOneOfStudyScope(grantedScopes, objectId);
    } else {
      val msg = String.format("Invalid access type '%s' found in Metadata record for object id: %s", fileAccessType,
        objectId);
      log.error(msg);
      throw new NotRetryableException(new IllegalArgumentException(msg));
    }
  }

}
