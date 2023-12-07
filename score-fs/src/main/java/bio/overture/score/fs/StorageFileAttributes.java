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
package bio.overture.score.fs;

import static java.util.regex.Pattern.compile;

import bio.overture.score.core.model.IndexFileType;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class StorageFileAttributes implements PosixFileAttributes {

  private final Pattern REGULAR_FILE_PATTERN =
      compile("/" + "[^/]*/" + ".+"); // <root>/ <dir>/ <file>

  private static final FileTime DEFAULT_TIME = FileTime.fromMillis(System.currentTimeMillis());

  /** Configuration. */
  @NonNull private final StoragePath path;

  /** Metadata. */
  @NonNull private final StorageContext context;

  @Override
  public FileTime lastModifiedTime() {
    val real = path.getFile().isPresent();
    return real ? FileTime.fromMillis(path.getFile().get().getLastModified()) : DEFAULT_TIME;
  }

  @Override
  public FileTime lastAccessTime() {
    return lastModifiedTime();
  }

  @Override
  public FileTime creationTime() {
    return lastModifiedTime();
  }

  @Override
  public boolean isRegularFile() {
    // FIXME: Tempfix for samtools
    // https://github.com/samtools/samtools/blob/834bf7e9c9d4bbf7d60160138f728e7968373e3a/sam_view.c#L450
    if (path.endsWith(".csi")) {
      return false;
    }

    if (context.getLayout() == StorageFileLayout.BUNDLE) {
      return matches(path, REGULAR_FILE_PATTERN);
    } else {
      return !path.toString().equals("/");
    }
  }

  @Override
  public boolean isDirectory() {
    // FIXME: Tempfix for samtools
    // https://github.com/samtools/samtools/blob/834bf7e9c9d4bbf7d60160138f728e7968373e3a/sam_view.c#L450
    if (path.endsWith(".csi")) {
      return false;
    }

    return !isRegularFile();
  }

  @Override
  public boolean isSymbolicLink() {
    if (context.getLayout() == StorageFileLayout.OBJECT_ID) {
      return IndexFileType.isIndexFile(path.getFileName().toString());
    }
    return false;
  }

  @Override
  public boolean isOther() {
    return false;
  }

  @Override
  public long size() {
    return path.getFile().isPresent() ? path.getFile().get().getSize() : 0;
  }

  @Override
  public Object fileKey() {
    return path.toAbsolutePath().toString();
  }

  @Override
  public UserPrincipal owner() {
    return null;
  }

  @Override
  public GroupPrincipal group() {
    return null;
  }

  @Override
  public Set<PosixFilePermission> permissions() {
    return ImmutableSet.of(PosixFilePermission.OWNER_READ);
  }

  private static boolean matches(Path path, Pattern patern) {
    return patern.matcher(path.toString()).matches();
  }
}
