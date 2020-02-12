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
package bio.overture.score.client.command;

import bio.overture.score.client.cli.Terminal;
import bio.overture.score.client.config.ClientProperties;
import bio.overture.score.client.manifest.DownloadManifest;
import bio.overture.score.client.util.ProfileRepoValidator;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Abstract class to handle command line arugments
 */
public abstract class AbstractClientCommand implements ClientCommand {

  /**
   * Dependencies.
   */
  @Autowired
  protected ClientProperties properties;
  @Autowired
  protected Terminal terminal;
  @Value("${storage.profile}")
  protected String storageProfile;

  protected void printTitle() {
    terminal.printStatus("\n" + terminal.label("> ") + terminal.value("SCORE ") + "Client\n\n");
  }

  protected void validateManifest(final DownloadManifest manifest) {
    for (val entry : manifest.getEntries()) {
      try {
        if (!ProfileRepoValidator.validateRepoAgainstProfile(storageProfile, entry.getRepoCode())) {
          terminal
              .printWarn(
                  "Manifest entry %s - %s (%s) exists in the '%s' repository. However, this SCORe Client instance is using the '%s' profile.",
                  entry.getFileId(), entry.getFileName(), entry.getFileUuid(), entry.getRepoCode(), storageProfile);
        }
      } catch (IllegalArgumentException iae) {
        terminal.printWarn(iae.getMessage());
      }
    }
  }

}
