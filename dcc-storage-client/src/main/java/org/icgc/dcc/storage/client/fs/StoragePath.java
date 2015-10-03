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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;

/**
 * @see https://github.com/semiosis/glusterfs-java-filesystem/blob/f6fbaa27f15d8226f06ca34c51dadc91486e49af/glusterfs-
 * java-filesystem/src/main/java/com/peircean/glusterfs/GlusterPath.java
 */
@Data
@EqualsAndHashCode(exclude = "pathString")
public class StoragePath implements Path {

  private final StorageFileSystem fileSystem;
  private final String[] parts;

  private String pathString;
  private boolean absolute;

  public StoragePath(StorageFileSystem fileSystem, String path) {
    if (null == fileSystem) {
      throw new IllegalArgumentException("fileSystem can not be empty");
    }
    if (null == path) {
      throw new InvalidPathException("", "path can not be null");
    }
    this.fileSystem = fileSystem;
    this.pathString = path;

    String stripped = path;
    if (path.startsWith(fileSystem.getSeparator())) {
      absolute = true;
      stripped = stripped.substring(1);
    }
    if (stripped.endsWith(fileSystem.getSeparator())) {
      stripped = stripped.substring(0, stripped.length() - 1);
    }
    parts = stripped.split(fileSystem.getSeparator());
  }

  StoragePath(StorageFileSystem fileSystem, String[] parts, boolean absolute) {
    this.fileSystem = fileSystem;
    this.parts = parts;
    this.absolute = absolute;
  }

  @Override
  public boolean isAbsolute() {
    return absolute;
  }

  @Override
  public Path getRoot() {
    if (absolute) {
      return fileSystem.getRootDirectories().iterator().next();
    } else {
      return null;
    }
  }

  @Override
  public Path getFileName() {
    if (parts.length == 0 || parts[0].isEmpty()) {
      return null;
    } else {
      return new StoragePath(fileSystem, parts[parts.length - 1]);
    }
  }

  @Override
  public Path getParent() {
    if (parts.length <= 1 || parts[0].isEmpty()) {
      if (absolute) {
        return getRoot();
      } else {
        return null;
      }
    } else {
      return new StoragePath(fileSystem, Arrays.copyOfRange(parts, 0, parts.length - 1), absolute);
    }
  }

  @Override
  public int getNameCount() {
    if (parts.length <= 1 && parts[0].isEmpty()) {
      if (absolute) {
        return 0;
      } else {
        throw new IllegalStateException("Only the root path should have one empty part");
      }
    } else {
      return parts.length;
    }
  }

  @Override
  public Path getName(int i) {
    if (i < 0 || i >= parts.length || (0 == i && parts.length <= 1 && parts[0].isEmpty())) {
      throw new IllegalArgumentException("invalid name index");
    }
    return new StoragePath(fileSystem, Arrays.copyOfRange(parts, 0, i + 1), absolute);
  }

  @Override
  public Path subpath(int i, int i2) {
    if ((0 == i && parts.length <= 1 && parts[0].isEmpty())
        || i < 0 || i2 < 0
        || i >= parts.length || i2 > parts.length
        || i > i2) {
      throw new IllegalArgumentException("invalid indices");
    }
    return new StoragePath(fileSystem, Arrays.copyOfRange(parts, i, i2), absolute);
  }

  @Override
  public boolean startsWith(Path path) {
    StoragePath otherPath = (StoragePath) path;
    if (this.equals(otherPath)) {
      return true;
    }
    if (otherPath.getParts().length > parts.length) {
      return false;
    }
    if (absolute && otherPath.isAbsolute() && otherPath.getParts()[0].isEmpty()) {
      return true;
    }
    String[] thisPrefix = Arrays.copyOfRange(parts, 0, otherPath.getParts().length);
    return ((absolute == otherPath.isAbsolute())
        && (Arrays.equals(thisPrefix, otherPath.getParts())));
  }

  @Override
  public boolean startsWith(String s) {
    return startsWith(new StoragePath(fileSystem, s));
  }

  @Override
  public boolean endsWith(Path path) {
    StoragePath otherPath = (StoragePath) path;
    if (this.equals(otherPath)) {
      return true;
    }
    if (otherPath.getParts().length > parts.length) {
      return false;
    }
    if (absolute && otherPath.isAbsolute() && otherPath.getParts()[0].isEmpty()) {
      return true;
    }
    String[] thisSuffix = Arrays.copyOfRange(parts, parts.length - otherPath.getParts().length, parts.length);
    return ((!otherPath.isAbsolute())
        && (Arrays.equals(thisSuffix, otherPath.getParts())));
  }

  @Override
  public boolean endsWith(String s) {
    return toString().endsWith(s);
  }

  @Override
  public Path normalize() {
    List<String> newParts = new LinkedList<String>();
    for (String part : parts) {
      if (part.equals("..")) {
        newParts.remove(newParts.size() - 1);
      } else if (!part.equals(".") && !part.isEmpty()) {
        newParts.add(part);
      }
    }
    return new StoragePath(fileSystem, newParts.toArray(new String[] {}), absolute);
  }

  @Override
  public Path resolve(Path path) {
    StoragePath otherPath = (StoragePath) path;
    if (!fileSystem.equals(otherPath.getFileSystem())) {
      throw new IllegalArgumentException("Can not resolve other path because it's on a different filesystem");
    }

    if (otherPath.isAbsolute() || (absolute && parts.length == 1 && parts[0].isEmpty())) {
      return new StoragePath(fileSystem, otherPath.getParts(), true);
    }

    if (otherPath.getParts().length == 1 && otherPath.getParts()[0].isEmpty()) {
      return this;
    }

    String[] newParts = Arrays.copyOf(parts, parts.length + otherPath.getParts().length);
    System.arraycopy(otherPath.getParts(), 0, newParts, parts.length, otherPath.getParts().length);
    return new StoragePath(fileSystem, newParts, absolute);
  }

  @Override
  public Path resolve(String s) {
    return resolve(new StoragePath(fileSystem, s));
  }

  @Override
  public Path resolveSibling(Path path) {
    return getParent().resolve(path);
  }

  @Override
  public Path resolveSibling(String s) {
    return getParent().resolve(s);
  }

  @Override
  public Path relativize(Path path) {
    if (!fileSystem.equals(path.getFileSystem())) {
      throw new IllegalArgumentException("Can not relativize other path because it's on a different filesystem");
    }

    if (!this.isAbsolute() || !path.isAbsolute()) {
      throw new IllegalArgumentException("Can only relativize when both paths are absolute");
    }

    StoragePath other = (StoragePath) path;
    List<String> relativeParts = new LinkedList<String>();
    boolean stillCommon = true;
    int lastCommonName = -1;
    for (int i = 0; i < parts.length; i++) {
      if (i >= other.getParts().length) {
        for (int r = 0; r < other.getParts().length; r++) {
          relativeParts.add("..");
        }
        break;
      }
      if (stillCommon && parts[i].equals(other.getParts()[i])) {
        lastCommonName = i;
      } else {
        stillCommon = false;
        relativeParts.add("..");
      }
    }
    for (int i = lastCommonName + 1; i < other.getParts().length; i++) {
      relativeParts.add(other.getParts()[i]);
    }
    return new StoragePath(fileSystem, relativeParts.toArray(new String[] {}), false);
  }

  @Override
  public URI toUri() {
    try {
      val fs = getFileSystem();
      return new URI(fs.provider().getScheme(), null, toString(), null, null);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public Path toAbsolutePath() {
    if (!absolute) {
      throw new UnsupportedOperationException();
    } else {
      return this;
    }
  }

  @Override
  public Path toRealPath(LinkOption... linkOptions) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public File toFile() {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watchService, WatchEvent.Kind<?>[] kinds, WatchEvent.Modifier... modifiers)
      throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public WatchKey register(WatchService watchService, WatchEvent.Kind<?>... kinds) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<Path> iterator() {
    List<Path> list = new ArrayList<Path>(parts.length);
    if (parts.length >= 1 && !parts[0].isEmpty()) {
      for (String p : parts) {
        list.add(new StoragePath(fileSystem, p));
      }
    }
    return Collections.unmodifiableList(list).iterator();
  }

  @Override
  public int compareTo(Path path) {
    if (!getClass().equals(path.getClass())) {
      throw new ClassCastException();
    }
    if (!fileSystem.equals(path.getFileSystem())) {
      throw new IllegalArgumentException("Can not compare other path because it's on a different filesystem");
    }
    StoragePath other = (StoragePath) path;
    String[] otherParts = other.getParts();
    for (int i = 0; i < Math.min(parts.length, otherParts.length); i++) {
      int c = parts[i].compareTo(otherParts[i]);
      if (c != 0) {
        return c;
      }
    }
    return parts.length - otherParts.length;
  }

  @Override
  public String toString() {
    return getString();
  }

  public String getString() {
    if (null != pathString) {
      return pathString;
    } else {
      StringBuilder sb = new StringBuilder((absolute ? fileSystem.getSeparator() : ""));
      for (String p : parts) {
        sb.append(p).append(fileSystem.getSeparator());
      }
      sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
    }
  }

}