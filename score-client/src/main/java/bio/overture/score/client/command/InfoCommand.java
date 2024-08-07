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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.stereotype.Component;

@Component
@Parameters(separators = "=", commandDescription = "Display application configuration information")
public class InfoCommand extends AbstractClientCommand {

  /** Options. */
  @Parameter(names = "--verbose", description = "Show all configuration property sources")
  private boolean verbose = false;

  /** Dependencies. */
  @Value("${storage.url}")
  private String storageUrl;

  @Value("${metadata.url}")
  private String metadataUrl;

  @Autowired private StandardEnvironment env;

  @Override
  public int execute() throws Exception {
    printTitle();
    version();
    return SUCCESS_STATUS;
  }

  private void version() {
    active();

    if (verbose) {
      terminal.println("");
      sources();
    }
  }

  private void active() {
    if ((storageUrl.isEmpty() || storageUrl == null) && (metadataUrl.isEmpty() || metadataUrl == null)) {
      throw new IllegalArgumentException("Error: Storage URL or Metadata URL is not configured. Please provide a valid URL.");
    }
    terminal.println(terminal.label("  Active Configuration: "));
    terminal.println("    Profile:          " + storageProfile);
    terminal.println("    Storage URL: " + terminal.url(storageUrl));
    terminal.println("    Metadata URL:" + terminal.url(metadataUrl));
    terminal.println("    Access Token:     " + properties.getAccessToken());
  }
  private void sources() {
    terminal.println(terminal.label("  Configuration Sources: "));
    for (val source : env.getPropertySources()) {
      if (source instanceof SystemEnvironmentPropertySource
          || source instanceof RandomValuePropertySource) {
        // Skip because this will cause issues with terminal display or is useless
        continue;
      }

      terminal.println("    " + terminal.value(source.getName()));
      if (source instanceof EnumerablePropertySource) {
        val enumerable = (EnumerablePropertySource<?>) source;
        for (val propertyName :
            Sets.newTreeSet(ImmutableSet.copyOf(enumerable.getPropertyNames()))) {
          if (!propertyName.equalsIgnoreCase("accessToken")) {
            terminal.println(
                "      - " + propertyName + ": " + enumerable.getProperty(propertyName));
          }
        }
      }
    }
  }
}
