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
package org.icgc.dcc.storage.client.download;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.state.TransferState;
import org.icgc.dcc.storage.client.util.PresignedUrlValidator;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class DownloadStateStore extends TransferState {

  @Autowired
  private PresignedUrlValidator urlValidator;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  public void init(File stateDir, ObjectSpecification spec) {
    log.debug("Download Specification : {}", spec);
    try {
      byte[] content = MAPPER.writeValueAsBytes(spec);
      File objectStateDir = getObjectStateDir(stateDir, spec.getObjectId());

      log.debug("About to delete {}", objectStateDir.toString());
      deleteDirectoryIfExist(objectStateDir);
      log.debug("About to re-create {}", objectStateDir.toString());
      Files.createDirectories(objectStateDir.toPath());
      File specFile = new File(objectStateDir, getSpecificationName());
      log.debug("About to copy content to {}", specFile.toString());
      Files.copy(new ByteArrayInputStream(content), specFile.toPath());
      log.debug("Finished writing specification to {}", specFile.toPath().toString());
    } catch (IOException e) {
      log.error("Failed to create meta file", e);
      throw new NotRetryableException(e);
    }
  }

  private String getPartName(Part part) {
    return String.format("%s%08x", getPartPrefix(), (0xFFFFFFFF & part.getPartNumber()));
  }

  private String getPartPrefix() {
    return "part-";
  }

  protected String getSpecificationName() {
    return "meta";
  }

  public boolean hasStarted(File stateDir, String objectId) {
    return Files.exists(new File(getObjectStateDir(stateDir, objectId), getSpecificationName()).toPath());
  }

  public ObjectSpecification getProgress(File stateDir, String objectId) throws IOException {
    log.debug("Loading local progress for {} from {}", objectId, stateDir.toString());
    val spec = loadSpecification(stateDir, objectId);
    log.debug("Completed loading local object specification (meta file)");
    for (val part : spec.getParts()) {
      log.debug("Checking md5 for part {}", part.getPartNumber());
      if (isCompleted(stateDir, objectId, part)) {
        val completedPart = loadPart(stateDir, objectId, getPartName(part));
        // Copy download md5 into ObjectSpecification
        part.setMd5(completedPart.getMd5());
      } else {
        // Part is not complete - check if it has expired
        if (urlValidator.isUrlExpired(part.getUrl())) {
          val ise =
              new IllegalStateException(
                  "Presigned URL's have expired because download was not completed in allotted period. Restarting.");
          throw new NotRetryableException(ise);
        }
      }
    }
    return spec;
  }

  /**
   * A marker file is written to the working directory after each part is downloaded and the MD5 checksum is verified.
   * Checking to see if a part was completed previously is based on the presence of this marker file.
   * @param stateDir - working directory for download
   * @param objectId - object id
   * @param part - metadata about part - really just for part number
   * @return
   */
  private boolean isCompleted(File stateDir, String objectId, Part part) {
    val partFile = new File(getObjectStateDir(stateDir, objectId), getPartName(part));
    return partFile.exists();
  }

  public void commit(File stateDir, String objectId, Part part) {
    log.debug("Attempting to commit {} part {} to {}", objectId, part.getPartNumber(), stateDir.toString());
    try {
      byte[] content = MAPPER.writeValueAsBytes(part);
      val partFile = new File(getObjectStateDir(stateDir, objectId), getPartName(part));
      Files.copy(new ByteArrayInputStream(content), partFile.toPath());
      log.debug("Copied part {} to {}", part.getPartNumber(), partFile.toPath().toString());
    } catch (IOException e) {
      log.error("Failed to create meta file {} ", stateDir.getAbsolutePath(), e);
      throw new NotRetryableException(e);
    }
  }

  private Part loadPart(File stateDir, String objectId, String partFileName) {
    File objectStateDir = getObjectStateDir(stateDir, objectId);
    File partFile = new File(objectStateDir, partFileName);

    return readPart(partFile);
  }

  protected Part readPart(File partFile) {
    try {
      val part = MAPPER.readValue(partFile, Part.class);
      return part;
    } catch (IOException e) {
      throw new NotRetryableException(e);
    }
  }

  public ObjectSpecification loadSpecification(File stateDir, String objectId) {
    File objectStateDir = getObjectStateDir(stateDir, objectId);
    File specFile = new File(objectStateDir, getSpecificationName());

    return readMeta(specFile);
  }

  protected ObjectSpecification readMeta(File specFile) {
    try {
      val spec = MAPPER.readValue(specFile, ObjectSpecification.class);
      return spec;
    } catch (IOException e) {
      throw new NotRetryableException(e);
    }
  }

  public void deletePart(File stateDir, String objectId, Part part) {
    val partStateFile = new File(getObjectStateDir(stateDir, objectId), getPartName(part));
    try {
      partStateFile.delete();
    } catch (Throwable e) {
      throw new NotRetryableException(e);
    }
  }

  public long getObjectSize(File stateDir, String objectId) {
    val spec = loadSpecification(stateDir, objectId);
    return spec.getObjectSize();
  }

  public boolean canFinalize(File outDir, String objectId) {
    val spec = loadSpecification(outDir, objectId);
    for (val part : spec.getParts()) {
      if (!isCompleted(outDir, objectId, part)) {
        return false;
      }
    }
    return true;
  }
}
