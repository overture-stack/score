/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package collaboratory.storage.object.store.client.cli.command;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import collaboratory.storage.object.store.client.download.ObjectDownload;
import lombok.SneakyThrows;

/**
 * Handle download command line arguments
 */
@Component
@Parameters(separators = "=", commandDescription = "object to download")
public class DownloadCommand extends AbstractClientCommand {

  @Parameter(names = "--out-dir", description = "Path to an output directory", required = true)
  private String filePath;

  @Parameter(names = "-f", description = "force to re-upload", required = false)
  private boolean isForce = false;

  @Parameter(names = "--object-id", description = "object id to download", required = true)
  private String oid;

  @Parameter(names = "--offset", description = "the offset to set for download", required = false)
  private long offset = 0;

  @Parameter(names = "--length", description = "the length of the download", required = false)
  private long length = -1;

  @Autowired
  private ObjectDownload downloader;

  @Override
  @SneakyThrows
  public int execute() {
    println("Start downloading object: %s", oid);
    File dir = new File(filePath);
    downloader.download(dir, oid, offset, length, isForce);

    return SUCCESS_STATUS;
  }

}
