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

import static bio.overture.score.core.util.VersionUtils.getScmInfo;
import static com.google.common.base.MoreObjects.firstNonNull;

import com.beust.jcommander.Parameters;
import org.springframework.stereotype.Component;

@Component
@Parameters(separators = "=", commandDescription = "Display application version information")
public class VersionCommand extends AbstractClientCommand {

  /** Constants. */
  private static final String SUPPORT_EMAIL = "";

  @Override
  public int execute() throws Exception {
    printTitle();
    version();
    return SUCCESS_STATUS;
  }

  private void version() {
    terminal.println(terminal.label("  Version: ") + getVersion());
    terminal.println(terminal.label("  Built:   ") + getScmInfo().get("git.build.time"));
  }

  private String getVersion() {
    return firstNonNull(getClass().getPackage().getImplementationVersion(), "[unknown version]");
  }
}
