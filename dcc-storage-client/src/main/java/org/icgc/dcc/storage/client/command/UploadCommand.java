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
package org.icgc.dcc.storage.client.command;

import static org.icgc.dcc.storage.client.cli.Parameters.checkParameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.icgc.dcc.storage.client.cli.FileValidator;
import org.icgc.dcc.storage.client.cli.ObjectIdValidator;
import org.icgc.dcc.storage.client.upload.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import lombok.Cleanup;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Parameters(separators = "=", commandDescription = "Upload file object(s) to the remote storage repository")
public class UploadCommand extends AbstractClientCommand {

  /**
   * Options.
   */
  @Parameter(names = "--file", description = "Path to file to upload", validateValueWith = FileValidator.class)
  private File file;
  @Parameter(names = "--manifest", description = "Path to manifest file", validateValueWith = FileValidator.class)
  private File manifestFile;
  @Parameter(names = "--force", description = "Force re-upload", required = false)
  private boolean isForce = false;
  @Parameter(names = "--object-id", description = "Object id assigned to upload file", validateValueWith = ObjectIdValidator.class)
  private String objectId;

  /**
   * Dependencies.
   */
  @Autowired
  private UploadService uploader;

  @Override
  public int execute() throws Exception {
    checkParameter(objectId != null || manifestFile != null, "One of --object-id or --manifest must be specified");

    terminal.print("\r");
    if (manifestFile != null) {
      val manifest = readManifest();
      for (val entry : manifest.entrySet()) {
        val objectId = (String) entry.getKey();
        val file = new File((String) entry.getValue());

        uploadFile(objectId, file);
      }
    } else {
      checkParameter(file != null, "--file must be specified if --object-id is specified");
      uploadFile(objectId, file);
    }

    return SUCCESS_STATUS;
  }

  private void uploadFile(String objectId, File file) throws IOException {
    log.info("Uploading file '{}'...", file);
    checkParameter(file.length() > 0,
        "File '%s' is empty. Uploads of empty files are not permitted. Aborting...", file.getCanonicalPath());

    val exists = uploader.isObjectExist(objectId);
    checkParameter(isForce || !exists,
        "Object id %s already exists remotely and --force was not specified. Aborting...%n", objectId);

    val warn = isForce && exists;
    if (warn) {
      terminal.printWarn("Object %s exists and --force specified. Overwritting...", objectId);
    }

    terminal.printf("Uploading object: '%s' using the object id %s%n", file, objectId);
    uploader.upload(file, objectId, isForce);
  }

  private Properties readManifest() throws IOException, FileNotFoundException {
    @Cleanup
    val inputStream = new FileInputStream(manifestFile);

    val manifest = new Properties();
    manifest.load(inputStream);

    return manifest;
  }

}
