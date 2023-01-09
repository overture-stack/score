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

import bio.overture.score.client.cli.ObjectIdValidator;
import bio.overture.score.client.download.DownloadService;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;

@Component
@Parameters(separators = "=", commandDescription = "Resolve the URL of a specified remote file object")
public class UrlCommand extends AbstractClientCommand {

  /**
   * Options.
   */
  @Parameter(names = "--object-id", description = "Object id to resolve URL for", required = true, validateValueWith = ObjectIdValidator.class)
  private String objectId;

  /**
   * Dependencies.
   */
  @Autowired
  private DownloadService downloader;

  @Override
  public int execute() throws Exception {

    terminal.printStatus("Resolving URL for object: " + terminal.value(objectId) + "\n");
    val url = downloader.getUrl(objectId);

    display(url);

    return SUCCESS_STATUS;
  }

  private void display(URL url) {
    System.out.println(url);
    System.out.flush();
  }

}
