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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import org.icgc.dcc.storage.client.fs.util.GlobPattern;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * See http://stackoverflow.com/questions/22966176/creating-a-custom-filesystem-implementation-in-java/32887126#32887126
 */
@RequiredArgsConstructor
public class StorageFileSystem extends FileSystem {

  /**
   * Constants.
   */
  public static final String SEPARATOR = "/";

  /**
   * Dependencies.
   */
  @Getter
  @NonNull
  private final StorageFileSystemProvider provider;

  @Override
  public FileSystemProvider provider() {
    return provider;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public String getSeparator() {
    return SEPARATOR;
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    return Collections.singleton(root());
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    return Collections.singleton(new StorageFileStore());
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    return null;
  }

  @Override
  public Path getPath(String first, String... more) {
    boolean absolute = first.startsWith(SEPARATOR);
    if (absolute) {
      first = first.substring(1);
    }

    String[] parts;
    if (null != more && more.length > 0) {
      parts = new String[1 + more.length];
      parts[0] = first;
      System.arraycopy(more, 0, parts, 1, more.length);
    } else {
      parts = new String[] { first };
    }

    return new StoragePath(this, parts, absolute);
  }

  public StoragePath root() {
    return new StoragePath(this, SEPARATOR);
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    checkArgument(syntaxAndPattern.contains(":"), "PathMatcher requires input syntax:expression. Got: '%s'",
        syntaxAndPattern);

    String[] parts = syntaxAndPattern.split(":", 2);
    Pattern pattern;
    if ("glob".equals(parts[0])) {
      pattern = GlobPattern.compile(parts[1]);
    } else if ("regex".equals(parts[0])) {
      pattern = Pattern.compile(parts[1]);
    } else {
      throw new UnsupportedOperationException("Unknown PathMatcher syntax: " + parts[0]);
    }

    return new StoragePathMatcher(pattern);
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    return null;
  }

  @Override
  public WatchService newWatchService() throws IOException {
    return null;
  }

}
