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

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.icgc.dcc.storage.core.model.IndexFileType;
import org.icgc.dcc.storage.fs.util.ReadOnlyFileSystemProvider;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * See http://stackoverflow.com/questions/22966176/creating-a-custom-filesystem-implementation-in-java/32887126#32887126
 */
@Slf4j
@RequiredArgsConstructor
public class StorageFileSystemProvider extends ReadOnlyFileSystemProvider {

  /**
   * Dependencies
   */
  @Getter
  @NonNull
  private final StorageContext context;

  /**
   * State.
   */
  private StorageFileSystem fileSystem;
  @SuppressWarnings("unused")
  private Map<String, ?> env;

  @Override
  public String getScheme() {
    return "icgc";
  }

  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    this.env = env;
    return this.fileSystem = new StorageFileSystem(this);
  }

  @Override
  @SneakyThrows
  public FileSystem getFileSystem(URI uri) {
    if (fileSystem == null) {
      val root = URI.create(StorageFileSystem.SEPARATOR);
      fileSystem = (StorageFileSystem) newFileSystem(root, null);
    }

    return fileSystem;
  }

  @Override
  public Path getPath(URI uri) {
    return new StoragePath(fileSystem, uri.getPath());
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException {
    log.debug("newByteChannel(path={}, options={}, attrs={})", path, options, Arrays.toString(attrs));

    return new StorageSeekableByteChannel((StoragePath) path, context);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path path, Filter<? super Path> filter) throws IOException {
    log.debug("newDirectoryStream(path={}, filter={})", path, filter);
    val files = getFiles();
    return new StorageDirectoryStream((StoragePath) path, context.getLayout(), filter, files);
  }

  @Override
  public boolean isSameFile(Path path1, Path path2) throws IOException {
    return path1.toAbsolutePath().toString().equals(path2.toAbsolutePath().toString());
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    if (context.getLayout() == StorageFileLayout.OBJECT_ID) {
      return IndexFileType.isIndexFile(path.getFileName().toString());
    }
    return false;
  }

  @Override
  public FileStore getFileStore(Path path) throws IOException {
    return new StorageFileStore();
  }

  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    log.debug("checkAccess(path={}, modes={})", path, Arrays.toString(modes));
    if (!context.isAuthorized()) {
      throw new AccessDeniedException("Not authorized with underlying storage system!");
    }

    // TODO: lookup path and verify existence instead
    // FIXME: Tempfix for samtools
    // https://github.com/samtools/samtools/blob/834bf7e9c9d4bbf7d60160138f728e7968373e3a/sam_view.c#L450
    if (path.endsWith(".csi")) {
      throw new NoSuchFileException("");
    }
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    log.debug("getFileAttributeView(path={}, type={}, options={})", path, type, Arrays.toString(options));
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
      throws IOException {
    log.debug("readAttributes(path={}, type={}, options={})", path, type, Arrays.toString(options));
    return (A) new StorageFileAttributes((StoragePath) path, context);
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    log.debug("readAttributes(path={}, attributes={}, options={})", path, attributes, Arrays.toString(options));
    return Collections.emptyMap();
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    log.debug("setAttribute(path={}, attribute={}, value={}, options={})", path, attribute, value,
        Arrays.toString(options));
  }

  private List<StorageFile> getFiles() {
    val files = context.getFiles();

    Comparator<StorageFile> comparison = null;
    if (context.getLayout() == StorageFileLayout.BUNDLE) {
      comparison = comparing(file -> file.getGnosId());
      comparison = comparison.thenComparing(file -> file.getFileName());
    } else if (context.getLayout() == StorageFileLayout.OBJECT_ID) {
      comparison = comparing(file -> file.getObjectId());
    }

    return files.stream().sorted(comparison).collect(toList());
  }

}
