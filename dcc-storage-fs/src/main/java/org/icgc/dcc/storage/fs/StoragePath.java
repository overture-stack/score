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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.icgc.dcc.storage.core.model.IndexFileType;
import org.icgc.dcc.storage.fs.util.GenericPath;

import lombok.val;

/**
 * @see https://github.com/semiosis/glusterfs-java-filesystem/blob/f6fbaa27f15d8226f06ca34c51dadc91486e49af/glusterfs-
 * java-filesystem/src/main/java/com/peircean/glusterfs/GlusterPath.java
 */
public class StoragePath extends GenericPath<StorageFileSystem> {

  private final StorageFileLayout layout;
  private final StorageContext context;

  public StoragePath(StorageFileSystem fileSystem, String path) {
    super(fileSystem, path);
    this.context = fileSystem.getProvider().getContext();
    this.layout = context.getLayout();
  }

  public StoragePath(StorageFileSystem fileSystem, String[] parts, boolean absolute) {
    super(fileSystem, parts, absolute);
    this.context = fileSystem.getProvider().getContext();
    this.layout = context.getLayout();
  }

  public Optional<StorageFile> getFile() {
    if (layout == StorageFileLayout.BUNDLE) {
      if (parts.length < 2) {
        return Optional.empty();
      }

      val gnosId = getGnosId();
      val fileName = getFilename();

      return context.getFilesByGnosId(gnosId).stream()
          .filter(file -> file.getFileName().equals(fileName))
          .findFirst();
    } else if (layout == StorageFileLayout.OBJECT_ID) {
      if (parts.length == 0) {
        return Optional.empty();
      }

      val fileName = getFilename();

      if (IndexFileType.isIndexFile(fileName)) {
        val indexFileType = IndexFileType.fromPath(fileName);
        // TODO: Verify if this works for other layout types other than `OBJECT_ID`!
        val objectId = IndexFileType.getFileName(fileName);
        return context.getIndexFile(objectId, indexFileType);
      }

      return Optional.ofNullable(context.getFile(fileName));
    }

    return null;
  }

  public String getGnosId() {
    if (layout == StorageFileLayout.BUNDLE) {
      if (parts.length < 1) {
        return null;
      }

      return parts[0];
    }

    return null;
  }

  @Override
  public Path getFileName() {
    if (layout == StorageFileLayout.BUNDLE) {
      if (parts.length == 0 || getGnosId().isEmpty()) {
        return null;
      } else {
        return new StoragePath(fileSystem, parts[parts.length - 1]);
      }
    } else if (layout == StorageFileLayout.OBJECT_ID) {
      if (parts.length == 0) {
        return null;
      } else {
        return new StoragePath(fileSystem, parts[parts.length - 1]);
      }
    }

    return null;
  }

  @Override
  public Path getParent() {
    if (layout == StorageFileLayout.BUNDLE) {
      if (parts.length <= 1 || getGnosId().isEmpty()) {
        if (absolute) {
          return getRoot();
        } else {
          return null;
        }
      } else {
        return new StoragePath(fileSystem, Arrays.copyOfRange(parts, 0, parts.length - 1), absolute);
      }
    } else if (layout == StorageFileLayout.OBJECT_ID) {
      if (parts.length <= 1) {
        if (absolute) {
          return getRoot();
        } else {
          return null;
        }
      } else {
        return new StoragePath(fileSystem, Arrays.copyOfRange(parts, 0, parts.length - 1), absolute);
      }
    }

    return null;
  }

  @Override
  public int getNameCount() {
    if (layout == StorageFileLayout.BUNDLE) {
      if (parts.length <= 1 && getGnosId().isEmpty()) {
        if (absolute) {
          return 0;
        } else {
          throw new IllegalStateException("Only the root path should have one empty part");
        }
      } else {
        return parts.length;
      }
    } else if (layout == StorageFileLayout.OBJECT_ID) {
      if (parts.length < 1) {
        if (absolute) {
          return 0;
        } else {
          throw new IllegalStateException("Only the root path should have one empty part");
        }
      } else {
        return parts.length;
      }
    }

    return 0;
  }

  @Override
  public Path getName(int i) {
    if (i < 0 || i >= parts.length
        || (0 == i && parts.length <= 1 && layout == StorageFileLayout.BUNDLE && getGnosId().isEmpty())) {
      throw new IllegalArgumentException("Invalid name index");
    }
    return createPath(fileSystem, Arrays.copyOfRange(parts, 0, i + 1), absolute);
  }

  @Override
  public Path subpath(int i, int i2) {
    if ((0 == i && parts.length <= 1 && layout == StorageFileLayout.BUNDLE && getGnosId().isEmpty())
        || i < 0 || i2 < 0
        || i >= parts.length || i2 > parts.length
        || i > i2) {
      throw new IllegalArgumentException("invalid indices");
    }
    return createPath(fileSystem, Arrays.copyOfRange(parts, i, i2), absolute);
  }

  @Override
  public Path resolve(Path path) {
    val otherPath = (StoragePath) path;
    if (!fileSystem.equals(otherPath.getFileSystem())) {
      throw new IllegalArgumentException("Can not resolve other path because it's on a different filesystem");
    }

    if (otherPath.isAbsolute() || (absolute && parts.length == 1 && getGnosId().isEmpty())) {
      return new StoragePath(fileSystem, otherPath.getParts(), true);
    }

    if (otherPath.getParts().length == 1 && otherPath.getParts()[0].isEmpty()) {
      return this;
    }

    String[] newParts = Arrays.copyOf(parts, parts.length + otherPath.getParts().length);
    System.arraycopy(otherPath.getParts(), 0, newParts, parts.length, otherPath.getParts().length);
    return createPath(fileSystem, newParts, absolute);
  }

  @Override
  public Iterator<Path> iterator() {
    List<Path> list = new ArrayList<>(parts.length);
    if (parts.length >= 1 && layout == StorageFileLayout.BUNDLE && !getGnosId().isEmpty()) {
      for (String p : parts) {
        list.add(createPath(fileSystem, p));
      }
    }
    return Collections.unmodifiableList(list).iterator();
  }

  public String getFilename() {
    if (layout == StorageFileLayout.BUNDLE) {
      if (parts.length < 2) {
        return null;
      }

      return parts[1];
    } else if (layout == StorageFileLayout.OBJECT_ID) {
      if (parts.length < 1) {
        return null;
      }

      return parts[0];
    }

    return null;
  }

  @Override
  protected GenericPath<?> createPath(StorageFileSystem fileSystem, String[] parts, boolean absolute) {
    return new StoragePath(fileSystem, parts, absolute);
  }

  @Override
  protected GenericPath<?> createPath(StorageFileSystem fileSystem, String path) {
    return new StoragePath(fileSystem, path);
  }

}