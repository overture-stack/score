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
package org.icgc.dcc.storage.fs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor
public class StorageDirectoryStream implements DirectoryStream<Path> {

  /**
   * Configuration.
   */
  @NonNull
  private final StoragePath path;
  @NonNull
  private final StorageFileLayout layout;
  @NonNull
  private final Filter<? super Path> filter;

  /**
   * Metadata.
   */
  @NonNull
  private final List<StorageFile> files;

  @Override
  public Iterator<Path> iterator() {
    if (layout == StorageFileLayout.BUNDLE) {
      if (isRoot()) {
        return listGnosDirs();
      } else {
        val gnosId = getGnosId();
        return listGnosDir(gnosId);
      }
    } else if (layout == StorageFileLayout.OBJECT_ID) {
      return listRoot();
    }

    return null;
  }

  @Override
  public void close() throws IOException {
    // Stateless
  }

  private String getGnosId() {
    return path.getParts()[0];
  }

  private boolean isRoot() {
    // Don't trust StoragePath#equals yet
    return path.toAbsolutePath().toString().equals("/");
  }

  private Iterator<Path> listGnosDirs() {
    return files.stream()
        .map(file -> file.getGnosId())
        .distinct()
        .map(this::gnosIdPath)
        .filter(this::filterPath).iterator();
  }

  private Iterator<Path> listGnosDir(String gnosId) {
    val objectFiles = files.stream()
        .filter(entity -> entity.getGnosId().equals(gnosId))
        .map(this::filePath)
        .filter(this::filterPath).iterator();

    return objectFiles;
  }

  private Iterator<Path> listRoot() {
    return files.stream()
        .map(this::filePath)
        .filter(this::filterPath).iterator();
  }

  private Path gnosIdPath(String gnosId) {
    return absolutePath(gnosId);
  }

  private Path filePath(StorageFile file) {
    if (layout == StorageFileLayout.BUNDLE) {
      return absolutePath(file.getGnosId(), file.getFileName());
    } else if (layout == StorageFileLayout.OBJECT_ID) {
      return absolutePath(file.getObjectId());
    }
    return null;
  }

  private Path absolutePath(String... parts) {
    return new StoragePath(path.getFileSystem(), parts, true);
  }

  @SneakyThrows
  private boolean filterPath(Path path) {
    return filter.accept(path);
  }

}