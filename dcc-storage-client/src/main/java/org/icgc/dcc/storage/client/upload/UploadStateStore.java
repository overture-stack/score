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
package org.icgc.dcc.storage.client.upload;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.state.TransferState;
import org.icgc.dcc.storage.core.model.ObjectSpecification;

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

  /*
   * We are using the directory that the upload file is currently located in. Requires read/write access to directory
   * Otherwise, we need to have user specify a working directory for upload - which is a bit too counter-intuitive.
   */
  @SneakyThrows
  public static File getContainingDir(@NonNull final File uploadFile) {
    val parentPath = Optional.ofNullable(uploadFile.getParent());
    if (parentPath.isPresent()) {
      return new File(parentPath.get());
    }
    return null;
  }

  /**
   * Write upload-id of current upload into state directory (hidden directory next to file being uploaded)
   * @param uploadFile - File we want to upload. Used to determine path to create temporary upload id directory
   * @param spec
   * @param force
   */
  public static void create(@NonNull File uploadFile, @NonNull ObjectSpecification spec, boolean force) {
    val filePath = getContainingDir(uploadFile);

    try {
      val uploadStateDir = getObjectStateDir(filePath, spec.getObjectId());

      removeDir(uploadStateDir, true);

      val uploadIdFile = new File(uploadStateDir, getStateName());
      try (PrintWriter out = new PrintWriter(uploadIdFile, StandardCharsets.UTF_8.name())) {
        out.println(spec.getUploadId());
      }
    } catch (IOException e) {
      log.error("Failed to create upload state file. Check permissions", e);
      throw new NotRetryableException(e);
    }
  }

  protected static String getStateName() {
    return "uploadid";
  }

  public static Optional<String> fetchUploadId(@NonNull File uploadFile, @NonNull String objectId) {
    Optional<String> result = Optional.ofNullable(null);
    val uploadStateDir = getObjectStateDir(getContainingDir(uploadFile), objectId);
    val uploadIdFile = new File(uploadStateDir, getStateName());

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
}
