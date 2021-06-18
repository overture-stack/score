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
package bio.overture.score.client.upload;

import bio.overture.score.client.exception.NotRetryableException;
import bio.overture.score.client.state.TransferState;
import bio.overture.score.core.model.ObjectSpecification;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * This really shouldn't have 'Store' in the class name - it has no state, so it's not storing anything. It's a
 * collection of utility methods to manipulate upload id's
 */
@Slf4j
public class UploadStateStore extends TransferState {

  /*
   * /path/to/ upload file dir /path/to/file.bam upload file /path/to/.<object-id>/ upload state dir
   * /path/to/.<object-id>/uploadid upload state file
   */

  @SneakyThrows
  public static String getContainingDir(@NonNull final File uploadFile) {
    return uploadFile.getParent();
  }

  /**
   * Write upload-id of current upload into state directory (hidden directory next to file being uploaded)
   * @param uploadStateParentDirPath - Path to create temporary upload id directory
   * @param spec
   */
  public static void create(@NonNull String uploadStateParentDirPath, @NonNull ObjectSpecification spec) throws NotRetryableException {
    try {
      val objectStatePath = getObjectStatePath(uploadStateParentDirPath, spec.getObjectId());
      val objectStateDir = new File(objectStatePath);

      removeDir(objectStateDir, true);

      val uploadIdFile = new File(objectStateDir, getStateName());
      try (PrintWriter out = new PrintWriter(uploadIdFile, StandardCharsets.UTF_8.name())) {
        out.println(spec.getUploadId());
      }
    } catch (IOException e) {
      log.error("Failed to create upload state file. Check permissions", e);
      throw new NotRetryableException(e);
    }
  }

  protected static String getStateName() {
    return "uploadId";
  }

  public static Optional<String> fetchUploadId(@NonNull String uploadStateParentDirPath, @NonNull String objectId) {
    Optional<String> result = Optional.empty();
    val objectStatePath = getObjectStatePath(uploadStateParentDirPath, objectId);
    val uploadIdFile = new File(objectStatePath, getStateName());

    if (uploadIdFile.exists()) {
      try (val reader =
          new BufferedReader(new InputStreamReader(new FileInputStream(uploadIdFile), StandardCharsets.UTF_8))) {
        result = Optional.ofNullable(reader.readLine());
        if (result.isPresent()) {
          // ...but is actually empty...
          if (result.get().isEmpty()) {
            return Optional.empty();
          }
        }
      } catch (IOException e) {
        throw new NotRetryableException(e);
      }
    }
    return result;
  }

  public static void close(@NonNull String uploadStateParentDirPath, @NonNull String objectId) throws IOException {
    val dirToDelete = new File(getObjectStatePath(uploadStateParentDirPath, objectId));
    deleteDirectoryIfExist(dirToDelete);
  }

  private static String getObjectStatePath(@NonNull String uploadStateParentDirPath, @NonNull String objectId) {
    return uploadStateParentDirPath +  "/." + objectId;
  }
}
