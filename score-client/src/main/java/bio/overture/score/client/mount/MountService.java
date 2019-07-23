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
package bio.overture.score.client.mount;

import co.paralleluniverse.javafs.JavaFS;
import com.google.common.collect.Maps;
import jnr.ffi.provider.ClosureManager;
import jnr.ffi.provider.jffi.NativeRuntime;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;

import static bio.overture.score.client.mount.MountOptions.parseOptions;
import static org.springframework.util.ReflectionUtils.findField;

@Service
public class MountService {

  /**
   * Configuration.
   */
  @Value("${mount.logging}")
  private boolean logging;

  /**
   * FUSE mount options
   * 
   * @see "http://sourceforge.net/p/fuse/fuse/ci/master/tree/README"s
   */
  public static final String INTERNAL_MOUNT_OPTIONS =
      // @formatter:off
      // Important: prevent async_read read-ahead which will cause multiple reconnects with HTTP backend
      "sync_read," + 

      // This option disables flushing the cache of the file contents on every open(2). This
      // should only be enabled on filesystems, where the file data is never changed
      // externally (i.e. not through the mounted FUSE filesystem).
      "kernel_cache," + 

      // Set the maximum number of bytes to read-ahead. he default is determined by the kernel. On linux-2.6.22 or
      // earlier it's 131072
      "max_readahead=1048576," + // 1 MB

      // Extend file metadata caching since the remote file system changes infrequently
      "entry_timeout=3600,"    + // 1 hr
      "negative_timeout=3600," + // 1 hr
      "attr_timeout=3600";       // 1 hr
      // @formatter:on

  public void mount(@NonNull FileSystem fileSystem, @NonNull Path mountPoint, Map<String, String> options)
      throws IOException, InterruptedException {

    val readOnly = true;
    JavaFS.mount(fileSystem, mountPoint, readOnly, logging, resolveOptions(options));
  }

  /**
   * To force unmount: {@code diskutil unmount force <mount point>}.
   */
  public void unmount(@NonNull Path mountPoint) throws IOException {
    JavaFS.unmount(mountPoint);
  }

  private Map<String, String> resolveOptions(Map<String, String> additionalOptions) {
    val combined = Maps.<String, String> newLinkedHashMap();
    combined.putAll(additionalOptions);
    combined.putAll(parseOptions(INTERNAL_MOUNT_OPTIONS));

    return combined;
  }

}
