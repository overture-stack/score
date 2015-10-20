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
package org.icgc.dcc.storage.client.mount;

import static org.springframework.util.ReflectionUtils.findField;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.paralleluniverse.javafs.JavaFS;
import jnr.ffi.provider.ClosureManager;
import jnr.ffi.provider.jffi.NativeRuntime;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

@Service
public class MountService {

  /**
   * Configuration.
   */
  @Value("${mount.logging}")
  private boolean logging;

  /**
   * See http://sourceforge.net/p/fuse/fuse/ci/master/tree/README
   */
  private Map<String, String> options = new HashMap<String, String>() {

    {
      // Prevent async_read read-ahead which will cause multiple reconnects with HTTP backend
      put("sync_read", null);

      // This option disables flushing the cache of the file contents on every open(2). This
      // should only be enabled on filesystems, where the file data is never changed
      // externally (not through the mounted FUSE filesystem).
      put("kernel_cache", null);

      // Set the maximum number of bytes to read-ahead. he default is determined by the kernel. On linux-2.6.22 or
      // earlier it's 131072
      put("max_readahead", "1048576"); // 1 MB

      // File metadata caching
      put("entry_timeout", "3600"); // 1 hr
      put("negative_timeout", "3600"); // 1 hr
      put("attr_timeout", "3600"); // 1 hr
    }

  };

  public void mount(@NonNull FileSystem fileSystem, @NonNull Path mountPoint) throws IOException, InterruptedException {
    patchFfi();

    val readOnly = true;
    JavaFS.mount(fileSystem, mountPoint, readOnly, logging, options);
  }

  /**
   * To force unmount: {@code diskutil unmount force <mount point>}.
   */
  public void unmount(@NonNull Path mountPoint) throws IOException {
    JavaFS.unmount(mountPoint);
  }

  /**
   * Workaround for unfortunate system class loader usage.
   * 
   * @see https://github.com/jnr/jnr-ffi/issues/51
   */
  @SneakyThrows
  private void patchFfi() {
    ClosureManager closureManager = NativeRuntime.getInstance().getClosureManager();

    // Get inner class loader
    val classLoader = findField(closureManager.getClass(), "classLoader");
    classLoader.setAccessible(true);
    val asmClassLoader = classLoader.get(closureManager);

    // Update to use context class loader over the default system class loader
    val parent = findField(asmClassLoader.getClass(), "parent");
    parent.setAccessible(true);
    parent.set(asmClassLoader, Thread.currentThread().getContextClassLoader());
  }

}
