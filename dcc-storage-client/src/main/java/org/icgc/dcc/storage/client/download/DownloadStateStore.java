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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.state.TransferState;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class DownloadStateStore extends TransferState {

  public void init(File stateDir, ObjectSpecification spec) {
    log.debug("Download Specification : {}", spec);
    ObjectMapper mapper = new ObjectMapper();
    try {
      byte[] content = mapper.writeValueAsBytes(spec);
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
    ObjectSpecification spec = loadSpecification(stateDir, objectId);
    log.debug("Completed loading local object specification (meta file)");
    for (Part part : spec.getParts()) {
      log.debug("Checking md5 for part {}", part.getPartNumber());
      if (isCompleted(stateDir, objectId, part)) {
        Part completedPart = loadPart(stateDir, objectId, getPartName(part));
        // Copy download md5 into ObjectSpecification
        part.setMd5(completedPart.getMd5());
      }
    }
    return spec;
  }

  private boolean isCompleted(File stateDir, String objectId, Part part) {
    File partFile = new File(getObjectStateDir(stateDir, objectId), getPartName(part));
    return partFile.exists();
  }

  public void commit(File stateDir, String objectId, Part part) {
    log.debug("Attempting to commit {} part {} to {}", objectId, part.getPartNumber(), stateDir.toString());
    ObjectMapper mapper = new ObjectMapper();
    try {
      byte[] content = mapper.writeValueAsBytes(part);
      File partFile = new File(getObjectStateDir(stateDir, objectId), getPartName(part));
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

    ObjectMapper mapper = new ObjectMapper();
    Part part = null;
    try (FileInputStream partStream = new FileInputStream(partFile)) {
      part = mapper.readValue(partStream, Part.class);
    } catch (IOException e) {
      throw new NotRetryableException(e);
    }
    return part;
  }

  public ObjectSpecification loadSpecification(File stateDir, String objectId) {
    File objectStateDir = getObjectStateDir(stateDir, objectId);
    File specFile = new File(objectStateDir, getSpecificationName());

    ObjectMapper mapper = new ObjectMapper();
    ObjectSpecification spec = null;
    try (FileInputStream specStream = new FileInputStream(specFile)) {
      spec = mapper.readValue(specStream, ObjectSpecification.class);
    } catch (IOException e) {
      throw new NotRetryableException(e);
    }
    return spec;
  }

  public void deletePart(File stateDir, String objectId, Part part) {
    File partStateFile = new File(getObjectStateDir(stateDir, objectId), getPartName(part));
    try {
      partStateFile.delete();
    } catch (Throwable e) {
      throw new NotRetryableException(e);
    }
  }

  public long getObjectSize(File stateDir, String objectId) {
    ObjectSpecification spec = loadSpecification(stateDir, objectId);
    return spec.getObjectSize();
  }

  public boolean canFinalize(File outDir, String objectId) {
    ObjectSpecification spec = loadSpecification(outDir, objectId);
    for (Part part : spec.getParts()) {
      if (!isCompleted(outDir, objectId, part)) {
        return false;
      }
    }
    return true;
  }
}
