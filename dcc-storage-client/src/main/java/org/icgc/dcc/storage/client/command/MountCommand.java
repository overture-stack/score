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
package org.icgc.dcc.storage.client.command;

import static org.springframework.util.ReflectionUtils.findField;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.icgc.dcc.storage.client.cli.DirectoryValidator;
import org.icgc.dcc.storage.client.download.DownloadService;
import org.icgc.dcc.storage.client.fs.StorageFileService;
import org.icgc.dcc.storage.client.fs.StorageFileSystems;
import org.icgc.dcc.storage.client.metadata.MetadataClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import co.paralleluniverse.javafs.JavaFS;
import jnr.ffi.provider.ClosureManager;
import jnr.ffi.provider.jffi.NativeRuntime;
import lombok.SneakyThrows;
import lombok.val;

@Component
@Parameters(separators = "=", commandDescription = "Mounts a read-only file system view of the remote repository")
public class MountCommand extends AbstractClientCommand {

  /**
   * Options
   */
  @Parameter(names = "--mount-point", description = "the mount point of the file system", required = true, validateValueWith = DirectoryValidator.class)
  private File mountPoint;

  /**
   * Dependencies.
   */
  @Autowired
  private MetadataClient metadataClient;
  @Autowired
  private DownloadService downloadService;

  @Override
  @SneakyThrows
  public int execute() {
    try {
      print("\rIndexing remote files. Please wait...");
      val fileService = new StorageFileService(metadataClient, downloadService);
      fileService.getEntities(); // Eager-load and cache

      val fileSystem = StorageFileSystems.newFileSystem(fileService);

      print("\rMounting file system to '%s'...                      ", mountPoint.getAbsolutePath());
      mount(fileSystem, mountPoint.toPath());
      terminal.print(
          terminal.label(
              "\rSuccessfully mounted file system at '" + terminal.value(mountPoint.getAbsolutePath())
                  + "' and is now ready for use!"));
      Thread.sleep(Long.MAX_VALUE);
    } catch (InterruptedException e) {
      println("\rUnmounting file system...");
    }

    return SUCCESS_STATUS;
  }

  /**
   * To force unmount: {@code diskutil unmount}.
   */
  private void mount(FileSystem fileSystem, Path mountPoint) throws IOException, InterruptedException {
    prepareFfi();

    val readOnly = false;
    val logging = false;
    JavaFS.mount(fileSystem, mountPoint, readOnly, logging);
  }

  /**
   * @see https://github.com/jnr/jnr-ffi/issues/51
   */
  @SneakyThrows
  private void prepareFfi() {
    ClosureManager closureManager = NativeRuntime.getInstance().getClosureManager();
    val classLoader = findField(closureManager.getClass(), "classLoader");
    classLoader.setAccessible(true);
    val asmClassLoader = classLoader.get(closureManager);

    val parent = findField(asmClassLoader.getClass(), "parent");
    parent.setAccessible(true);
    parent.set(asmClassLoader, Thread.currentThread().getContextClassLoader());
  }

}
