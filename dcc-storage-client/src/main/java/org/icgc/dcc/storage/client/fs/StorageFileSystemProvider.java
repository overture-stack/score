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
package org.icgc.dcc.storage.client.fs;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
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
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import lombok.SneakyThrows;

/**
 * See http://stackoverflow.com/questions/22966176/creating-a-custom-filesystem-implementation-in-java/32887126#32887126
 */
public class StorageFileSystemProvider extends FileSystemProvider {

  private StorageFileSystem fileSystem;

  @Override
  public String getScheme() {
    return "icgc";
  }

  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    return this.fileSystem = new StorageFileSystem(this);
  }

  @Override
  @SneakyThrows
  public FileSystem getFileSystem(URI uri) {
    if (fileSystem == null) {
      fileSystem = (StorageFileSystem) newFileSystem(URI.create("/"), null);
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
    return new SeekableByteChannel() {

      @Override
      public boolean isOpen() {
        return false;
      }

      @Override
      public void close() throws IOException {
      }

      @Override
      public int write(ByteBuffer src) throws IOException {
        return 0;
      }

      @Override
      public SeekableByteChannel truncate(long size) throws IOException {
        return null;
      }

      @Override
      public long size() throws IOException {
        return 0;
      }

      @Override
      public int read(ByteBuffer dst) throws IOException {
        return 0;
      }

      @Override
      public SeekableByteChannel position(long newPosition) throws IOException {
        return null;
      }

      @Override
      public long position() throws IOException {
        return 0;
      }

    };
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path p, Filter<? super Path> filter) throws IOException {
    return new DirectoryStream<Path>() {

      @Override
      public Iterator<Path> iterator() {
        return Collections.emptyIterator();
      }

      @Override
      public void close() throws IOException {
      }

    };
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    throwReadOnly();
  }

  @Override
  public void delete(Path path) throws IOException {
    throwReadOnly();
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    throwReadOnly();
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    throwReadOnly();
  }

  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    return path.toAbsolutePath().equals(path2.toAbsolutePath());
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    return false;
  }

  @Override
  public FileStore getFileStore(Path path) throws IOException {
    return null;
  }

  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
      throws IOException {
    return (A) new PosixFileAttributes() {

      @Override
      public FileTime lastModifiedTime() {
        return creationTime();
      }

      @Override
      public FileTime lastAccessTime() {
        return creationTime();
      }

      @Override
      public FileTime creationTime() {
        return FileTime.fromMillis(0);
      }

      @Override
      public boolean isRegularFile() {
        return false;
      }

      @Override
      public boolean isDirectory() {
        return true;
      }

      @Override
      public boolean isSymbolicLink() {
        return false;
      }

      @Override
      public boolean isOther() {
        return false;
      }

      @Override
      public long size() {
        return 666;
      }

      @Override
      public Object fileKey() {
        return "/";
      }

      @Override
      public UserPrincipal owner() {
        return new UserPrincipal() {

          @Override
          public String getName() {
            return "icgc-user";
          }

        };
      }

      @Override
      public GroupPrincipal group() {
        return new GroupPrincipal() {

          @Override
          public String getName() {
            return "icgc-group";
          }

        };
      }

      @Override
      public Set<PosixFilePermission> permissions() {
        return ImmutableSet.of(PosixFilePermission.OWNER_READ);
      }

    };
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    return Collections.emptyMap();
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
  }

  private void throwReadOnly() throws ReadOnlyFileSystemException {
    throw new ReadOnlyFileSystemException();
  }

}
