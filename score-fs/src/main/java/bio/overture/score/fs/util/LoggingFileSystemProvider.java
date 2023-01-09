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
package bio.overture.score.fs.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LoggingFileSystemProvider extends FileSystemProvider {

  private final FileSystemProvider delegate;

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override
  public String getScheme() {
    log.info("getScheme");
    return delegate.getScheme();
  }

  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    log.info("newFileSystem(uri={}, env={})", uri, env);
    return delegate.newFileSystem(uri, env);
  }

  @Override
  public FileSystem getFileSystem(URI uri) {
    log.info("getFileSystem(uri={})", uri);
    return delegate.getFileSystem(uri);
  }

  @Override
  public String toString() {
    log.info("toString()");
    return delegate.toString();
  }

  @Override
  public Path getPath(URI uri) {
    log.info("getPath(uri={})", uri);
    return delegate.getPath(uri);
  }

  @Override
  public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
    log.info("newFileSystem(path={}, env={})", path, env);
    return delegate.newFileSystem(path, env);
  }

  @Override
  public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
    log.info("newInputStream(path={}, options={})", path, Arrays.toString(options));
    return delegate.newInputStream(path, options);
  }

  @Override
  public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
    log.info("newOutputStream(path={}, options={})", path, Arrays.toString(options));
    return delegate.newOutputStream(path, options);
  }

  @Override
  public FileChannel newFileChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException {
    log.info("newFileChannel(path={}, options={}, attrs={})", path, options, Arrays.toString(attrs));
    return delegate.newFileChannel(path, options, attrs);
  }

  @Override
  public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> options,
      ExecutorService executor, FileAttribute<?>... attrs) throws IOException {
    log.info("newAsynchronousFileChannel(path={}, options={}, executor={}. attrs={})", path, options, executor,
        Arrays.toString(attrs));
    return delegate.newAsynchronousFileChannel(path, options, executor, attrs);
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
      throws IOException {
    log.info("newByteChannel(path={}, options={}, attrs={})", path, options, Arrays.toString(attrs));
    return delegate.newByteChannel(path, options, attrs);
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
    log.info("newDirectoryStream(dir={}, filter={})", dir, filter);
    return delegate.newDirectoryStream(dir, filter);
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    log.info("createDirectory(dir={}, attrs={})", dir, Arrays.toString(attrs));
    delegate.createDirectory(dir, attrs);
  }

  @Override
  public void createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
    log.info("createSymbolicLink(link={}, attrs={})", link, Arrays.toString(attrs));
    delegate.createSymbolicLink(link, target, attrs);
  }

  @Override
  public void createLink(Path link, Path existing) throws IOException {
    log.info("createLink(link={}, existing={})", link, existing);
    delegate.createLink(link, existing);
  }

  @Override
  public void delete(Path path) throws IOException {
    log.info("delete(path={})", path);
    delegate.delete(path);
  }

  @Override
  public boolean deleteIfExists(Path path) throws IOException {
    log.info("deleteIfExists(path={})", path);
    return delegate.deleteIfExists(path);
  }

  @Override
  public Path readSymbolicLink(Path link) throws IOException {
    log.info("readSymbolicLink(link={})", link);
    return delegate.readSymbolicLink(link);
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    log.info("copy(source={}, target={}, options={})", source, target, Arrays.toString(options));
    delegate.copy(source, target, options);
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    log.info("move(source={}, target={}, options={})", source, target, Arrays.toString(options));
    delegate.move(source, target, options);
  }

  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    log.info("move(path={}, path2={})", path, path2);
    return delegate.isSameFile(path, path2);
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    log.info("isHidden(path={})", path);
    return delegate.isHidden(path);
  }

  @Override
  public FileStore getFileStore(Path path) throws IOException {
    log.info("getFileStore(path={})", path);
    return delegate.getFileStore(path);
  }

  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    log.info("checkAccess(path={}, modes={})", path, Arrays.toString(modes));
    delegate.checkAccess(path, modes);
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    log.info("checkAccess(path={}, type={}, options={})", path, type, Arrays.toString(options));
    return delegate.getFileAttributeView(path, type, options);
  }

  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
      throws IOException {
    log.info("readAttributes(path={}, type={}, options={})", path, type, Arrays.toString(options));
    return delegate.readAttributes(path, type, options);
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    log.info("readAttributes(path={}, attributes={}, options={})", path, attributes, Arrays.toString(options));
    return delegate.readAttributes(path, attributes, options);
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    log.info("setAttribute(path={}, value={}, options={})", path, value, Arrays.toString(options));
    delegate.setAttribute(path, attribute, value, options);
  }

}
