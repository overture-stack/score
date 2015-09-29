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

import java.io.File;
import java.io.FileInputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.icgc.dcc.storage.client.cli.FileValidator;
import org.icgc.dcc.storage.client.cli.ObjectIdValidator;
import org.icgc.dcc.storage.client.upload.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle upload command line arguments
 */
@Component
@Parameters(separators = "=", commandDescription = "Uploads object(s) to remote storage")
@Slf4j
public class UploadCommand extends AbstractClientCommand {

  @Parameter(names = "--file", description = "path to file to upload", required = false, validateValueWith = FileValidator.class)
  private File file;

  @Parameter(names = "--manifest", description = "path to manifest file", required = false, validateValueWith = FileValidator.class)
  private File manifestFile;

  @Parameter(names = "--force", description = "force re-upload", required = false)
  private boolean isForce = false;

  @Parameter(names = "--object-id", description = "object id assigned to upload file", required = false, validateValueWith = ObjectIdValidator.class)
  private String oid;

  @Autowired
  private UploadService uploader;

  @Override
  @SneakyThrows
  public int execute() {
    if (file != null) {
      println("Start uploading file: %s", file);
      log.info("file: {}", file);
      if (file.length() == 0) {
        throw new IllegalArgumentException("Upload file '" + file.getCanonicalPath()
            + "' is empty. Uploads of empty files are not permitted. Aborting...");
      }

      uploader.upload(file, oid, isForce);

      return SUCCESS_STATUS;
    }

    Properties props = new Properties();
    props.load(new FileInputStream(manifestFile));
    for (Entry<Object, Object> entry : props.entrySet()) {
      String objectId = (String) entry.getKey();
      File obj = new File((String) entry.getValue());
      if ((isForce) || (!uploader.isObjectExist(objectId))) {
        println("Start uploading object: %s using the object id: %s", obj, objectId);
        uploader.upload(obj, objectId, isForce);
      } else {
        println("Object id: %s has been uploaded. Skipped.", objectId);
      }
    }

    return SUCCESS_STATUS;
  }
}
