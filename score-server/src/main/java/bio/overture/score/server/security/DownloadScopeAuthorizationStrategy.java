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
import org.springframework.security.core.Authentication;

@Slf4j
public class DownloadScopeAuthorizationStrategy extends UploadScopeAuthorizationStrategy {
  public DownloadScopeAuthorizationStrategy(@NonNull String studyPrefix, @NonNull String studySuffix,
    @NonNull String systemScope, MetadataService metadataService) {
    super(studyPrefix, studySuffix, systemScope, metadataService);
  }

  @Override
  public boolean authorize(Authentication authentication, @NonNull final String fileId) {
    val fileAccessType = fetchFileAccessType(fileId);
    val accessType = new Access(fileAccessType);

    if (accessType.isOpen()) {
      return true;
    } else if (accessType.isControlled()) {
      return super.authorize(authentication, fileId);
    } else {
      val msg = String.format("Invalid access type '%s' found in Metadata record for object id: %s", fileAccessType,
        fileId);
      log.error(msg);
      throw new NotRetryableException(new IllegalArgumentException(msg));
    }
  }
}
