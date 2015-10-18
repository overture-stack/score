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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.icgc.dcc.storage.fs.util.Formats.formatBytes;
import static org.icgc.dcc.storage.fs.util.Formats.formatBytesUnits;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.icgc.dcc.storage.client.cli.DirectoryValidator;
import org.icgc.dcc.storage.client.download.DownloadService;
import org.icgc.dcc.storage.client.manifest.ManfiestService;
import org.icgc.dcc.storage.client.manifest.ManifestResource;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.metadata.MetadataService;
import org.icgc.dcc.storage.client.mount.MountService;
import org.icgc.dcc.storage.client.mount.MountStorageContext;
import org.icgc.dcc.storage.client.transport.StorageService;
import org.icgc.dcc.storage.core.model.ObjectInfo;
import org.icgc.dcc.storage.fs.StorageFileSystems;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("experimental")
@Parameters(separators = "=", commandDescription = "Mounts a read-only file system view of the remote repository")
public class MountCommand extends AbstractClientCommand {

  /**
   * Options
   */
  @Parameter(names = "--mount-point", description = "the mount point of the file system", required = true, validateValueWith = DirectoryValidator.class)
  private File mountPoint;
  @Parameter(names = "--manifest", description = "manifest id (from the Data Portal), url or file path")
  private String manifestSpec;
  @Parameter(names = "--cache-metadata", description = "to speedup load times, cache metadata on disk locally and use if available")
  private boolean cacheMetadata;

  /**
   * Dependencies.
   */
  @Autowired
  private ManfiestService manfiestService;
  @Autowired
  private MetadataService metadataServices;
  @Autowired
  private StorageService storageService;
  @Autowired
  private DownloadService downloadService;
  @Autowired
  private MountService mountService;

  @Override
  public int execute() throws Exception {
    try {
      int i = 1;

      terminal.printStatus(i++, "Indexing remote entities. Please wait");
      val entities = terminal.printWaiting(this::resolveEntities);

      terminal.printStatus(i++, "Indexing remote objects. Please wait");
      List<ObjectInfo> objects = terminal.printWaiting(this::resolveObjects);
      if (hasManifest()) {
        // Manifest is a filtered view y'all!
        objects = filterManifestObjects(manifestSpec, objects);
      }

      terminal.printStatus(i++, "Checking access. Please wait");
      val context = new MountStorageContext(downloadService, entities, objects);
      if (!terminal.printWaiting(context::isAuthorized)) {
        terminal.printError("Access denied. Exiting...");
        return FAILURE_STATUS;
      }

      if (hasManifest()) {
        terminal.printStatus(i++, "Applying manifest view:\n");

        terminal.printLine();
        long totalSize = 0;
        for (val file : context.getFiles()) {
          terminal.println(" - " + file.toString());

          totalSize += file.getSize();
        }
        terminal.printLine();
        terminal.println(" Total size: " + formatBytes(totalSize) + " " + formatBytesUnits(totalSize) + "\n");
      }

      terminal.printStatus(i++, "Mounting file system to '" + mountPoint.getAbsolutePath() + "'...");
      val fileSystem = StorageFileSystems.newFileSystem(context);
      mountService.mount(fileSystem, mountPoint.toPath());

      terminal.printStatus(
          terminal.label(
              "Successfully mounted file system at " + terminal.value(mountPoint.getAbsolutePath())
                  + " and is now ready for use!"));

      // Let the user know we are done... when the time is right
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        terminal.printStatus(terminal.label("Shut down. Good bye!\n"));
      }));

      // Wait for interrupt
      Thread.sleep(Long.MAX_VALUE);
    } catch (InterruptedException ie) {
      return SUCCESS_STATUS;
    } catch (Exception e) {
      log.error("Unknown error:", e);
      throw e;
    }

    return SUCCESS_STATUS;
  }

  private List<ObjectInfo> resolveObjects() throws IOException {
    return resolveList("objects", () -> storageService.listObjects(), new TypeReference<List<ObjectInfo>>() {});
  }

  private List<Entity> resolveEntities() throws IOException {
    return resolveList("entities", () -> metadataServices.getEntities(), new TypeReference<List<Entity>>() {});
  }

  @SneakyThrows
  private <T> List<T> resolveList(String name, Supplier<List<T>> factory, TypeReference<List<T>> typeReference) {
    val cacheFile = new File("." + name + ".cache");
    if (cacheMetadata && cacheFile.exists()) {
      return new ObjectMapper().readValue(cacheFile, typeReference);
    }

    val values = factory.get();
    if (cacheMetadata) {
      new ObjectMapper().writeValue(cacheFile, values);
    }

    return values;
  }

  private boolean hasManifest() {
    return manifestSpec != null;
  }

  private List<ObjectInfo> filterManifestObjects(String manifestSpec, List<ObjectInfo> objects) {
    val manifest = manfiestService.getManifest(new ManifestResource(manifestSpec));

    val objectIds = manifest.getEntries().stream()
        .flatMap(entry -> Stream.of(entry.getFileUuid(), entry.getIndexFileUuid()))
        .collect(toSet());

    return objects.stream()
        .filter(object -> objectIds.contains(object.getId()))
        .collect(toList());
  }

}
