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
import java.io.FileInputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import collaboratory.storage.object.store.client.upload.ObjectUpload;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle upload command line arguments
 */
@Component
@Parameters(separators = "=", commandDescription = "file upload")
@Slf4j
public class UploadCommand extends AbstractClientCommand {

  @Parameter(names = "--file", description = "Path to a file", required = false)
  private String filePath;

  @Parameter(names = "--manifest", description = "Path to a manifest file", required = false)
  private File manifest;

  @Parameter(names = "-f", description = "force to re-upload", required = false)
  private boolean isForce = false;

  @Parameter(names = "--object-id", description = "object id assigned to the file", required = false)
  private String oid;

  @Autowired
  private ObjectUpload uploader;

  @Override
  @SneakyThrows
  public int execute() {

    if (filePath != null) {
      println("Start uploading file: %s", filePath);
      log.info("file: {}", filePath);
      File upload = new File(filePath);
      if (upload.length() == 0) {
        throw new IllegalArgumentException("Upload file '" + upload.getCanonicalPath()
            + "' is empty. Uploads of empty files are not permitted. Aborting...");
      }

      uploader.upload(upload, oid, isForce);
      return SUCCESS_STATUS;
    }

    Properties props = new Properties();
    props.load(new FileInputStream(manifest));
    for (Entry<Object, Object> entry : props.entrySet()) {
      String objectId = (String) entry.getKey();
      File obj = new File((String) entry.getValue());
      if (!uploader.isObjectExist(objectId)) {
        println("Start uploading object: %s using the object id: %s", obj, objectId);
        uploader.upload(obj, objectId, isForce);
      } else {
        println("Object id: %s has been uploaded. Skipped.", objectId);
      }
    }
    return SUCCESS_STATUS;
  }
}
