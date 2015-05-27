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
package collaboratory.storage.object.store.client.download;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.map.ObjectMapper;

import collaboratory.storage.object.store.client.upload.NotRetryableException;
import collaboratory.storage.object.store.core.model.ObjectSpecification;
import collaboratory.storage.object.store.core.model.Part;

@Slf4j
public class DownloadStateStore {

  public void init(File stateDir, ObjectSpecification spec) {
    log.debug("Download Specification : {}", spec);
    ObjectMapper mapper = new ObjectMapper();
    try {
      byte[] content = mapper.writeValueAsBytes(spec);
      File objectStateDir = getObjectStateDir(stateDir, spec.getObjectId());

      deleteDirectoryIfExist(objectStateDir);

      Files.createDirectories(objectStateDir.toPath());
      File specFile = new File(objectStateDir, getSpecificationName());
      Files.copy(new ByteArrayInputStream(content), specFile.toPath());
    } catch (IOException e) {
      log.error("Fail to create meta file", e);
      throw new NotRetryableException(e);
    }
  }

  /**
   * @param objectStateDir
   * @throws IOException
   */
  private void deleteDirectoryIfExist(File objectStateDir) throws IOException {
    if (objectStateDir.exists()) {
      Files.walkFileTree(objectStateDir.toPath(), new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file);
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        }

      });
    }

  }

  private File getObjectStateDir(File stateDir, String objectId) {
    return new File(stateDir, "." + objectId);
  }

  private String getSpecificationName() {
    return "meta";
  }

  private String getPartName(Part part) {
    return String.format("%s%08x", getPartPrefix(), (0xFFFFFFFF & part.getPartNumber()));
  }

  private String getPartPrefix() {
    return "part-";
  }

  public boolean hasStarted(File stateDir, String objectId) {
    return Files.exists(new File(getObjectStateDir(stateDir, objectId), getSpecificationName()).toPath());
  }

  public List<Part> getProgress(File stateDir, String objectId) throws IOException {
    ObjectSpecification spec = loadSpecification(stateDir, objectId);

    for (Part part : spec.getParts()) {
      if (isCompleted(stateDir, objectId, part)) {
        Part completedPart = loadPart(stateDir, objectId, getPartName(part));
        part.setMd5(completedPart.getMd5());
      }
    }
    return spec.getParts();
  }

  /**
   * @param stateDir
   * @param objectId
   * @param part
   * @return
   */
  private boolean isCompleted(File stateDir, String objectId, Part part) {
    File partFile = new File(getObjectStateDir(stateDir, objectId), getPartName(part));
    return partFile.exists();
  }

  public void commit(File stateDir, String objectId, Part part) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      byte[] content = mapper.writeValueAsBytes(part);
      File partFile = new File(getObjectStateDir(stateDir, objectId), getPartName(part));
      Files.copy(new ByteArrayInputStream(content), partFile.toPath());
    } catch (IOException e) {
      log.error("Fail to create meta file", e);
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
}
