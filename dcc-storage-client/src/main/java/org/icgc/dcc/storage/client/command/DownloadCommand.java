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
package org.icgc.dcc.storage.client.command;

import static java.util.stream.Collectors.toList;
import static org.icgc.dcc.common.core.util.Formats.formatBytes;
import static org.icgc.dcc.storage.client.cli.Parameters.checkParameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.storage.client.cli.ConverterFactory.OutputLayoutConverter;
import org.icgc.dcc.storage.client.cli.DirectoryValidator;
import org.icgc.dcc.storage.client.cli.ObjectIdValidator;
import org.icgc.dcc.storage.client.download.DownloadService;
import org.icgc.dcc.storage.client.manifest.ManfiestService;
import org.icgc.dcc.storage.client.manifest.ManifestResource;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.metadata.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Parameters(separators = "=", commandDescription = "Retrieve file object(s) from the remote storage repository")
public class DownloadCommand extends AbstractClientCommand {

  public enum OutputLayout {
    BUNDLE, FILENAME, ID
  }

  /**
   * Options
   */
  @Parameter(names = "--output-dir", description = "Path to output directory", required = true, validateValueWith = DirectoryValidator.class)
  private File outputDir;
  @Parameter(names = "--output-layout", description = "Layout of the output-dir. One of 'bundle' (nest files in bundle directory), 'filename' (nest files in filename directory), or 'id' (flat list of files named by their associated object id)", converter = OutputLayoutConverter.class)
  private OutputLayout layout = OutputLayout.FILENAME;
  @Parameter(names = "--force", description = "Force re-download (override local file)")
  private boolean force = false;
  @Parameter(names = "--manifest", description = "Path to manifest id, url or file")
  private ManifestResource manifestResource;
  @Parameter(names = "--object-id", description = "Object id to download", validateValueWith = ObjectIdValidator.class)
  private String objectId;
  @Parameter(names = "--offset", description = "The byte position in source file to begin download from")
  private long offset = 0;
  @Parameter(names = "--length", description = "The number of bytes to download")
  private long length = -1;
  @Parameter(names = "--index", description = "Download file index if available?", arity = 1)
  private boolean index = true;

  /**
   * Dependencies
   */
  @Autowired
  private ManfiestService manifestService;
  @Autowired
  private MetadataService metadataService;
  @Autowired
  private DownloadService downloadService;

  @Override
  public int execute() throws Exception {
    terminal.printStatus("Downloading...");
    checkParameter(objectId != null || manifestResource != null, "One of --object-id or --manifest must be specified");
    checkParameter(outputDir.exists(), "Output directory '%s' is missing", outputDir.getCanonicalPath());
    checkParameter(outputDir.canWrite(), "Cannot write to output dir '%s'. Please check permissions and try again",
        outputDir);

    val single = objectId != null;
    if (single) {
      // Ad-hoc single
      return downloadObjects(ImmutableList.of(objectId));
    } else {
      // Manifest based
      if (manifestResource.isGnosManifest()) {
        terminal
            .printError(
                "Manifest '%s' looks like a GNOS-format manifest file. Please ensure you are using a tab-delimited text file"
                    + " manifest from https://dcc.icgc.org/repositories",
                manifestResource.getValue());
        return FAILURE_STATUS;
      }
      val manifest = manifestService.getManifest(manifestResource);
      val entries = manifest.getEntries();
      if (entries.isEmpty()) {
        terminal.printError("Manifest '%s' is empty", manifestResource);
        return FAILURE_STATUS;
      }

      return downloadObjects(manifest.getEntries().stream().map(entry -> entry.getFileUuid()).collect(toList()));
    }
  }

  private int downloadObjects(List<String> objectIds) throws IOException {
    // Entities are defined in Meta service
    val entities = resolveEntities(objectIds);
    prepareLayout(entities);

    if (!verifyLocalAvailableSpace(entities)) {
      return FAILURE_STATUS;
    }

    int i = 1;
    terminal.println("");
    for (val entity : filterEntities(entities)) {
      terminal
          .printLine()
          .printf("[%s/%s] Downloading object: %s (%s)%n", i++, entities.size(), terminal.value(entity.getId()),
              entity.getFileName())
          .printLine();
      downloadService.download(outputDir, entity.getId(), offset, length, force);
      layoutFile(entity);
      terminal.println("");
    }

    return SUCCESS_STATUS;
  }

  /**
   * Prepares the local file system for moving files into after they have completed downloading.
   */
  @SneakyThrows
  private void prepareLayout(Set<Entity> entities) {
    for (val entity : entities) {
      val file = getLayoutTarget(entity);
      if (file.exists()) {
        if (force) {
          terminal.printWarn("File '%s' exists and --force specified. Removing...", file.getCanonicalPath());
          checkParameter(file.delete(), "Could not delete '%s'", file.getCanonicalPath());
        } else {
          terminal.printWarn("File '%s' exists and --force not specified. Skipping...", file.getCanonicalPath());
          continue;
        }
      }
    }
  }

  /**
   * Move the entity into its final destination.
   */
  private void layoutFile(Entity entity) throws IOException {
    val source = getLayoutSource(entity);
    val target = getLayoutTarget(entity);
    if (target.equals(source)) {
      return;
    }

    val targetDir = target.getParentFile();
    checkParameter(targetDir.exists() || targetDir.mkdirs(), "Could not create layout target directory %s", targetDir);
    checkParameter(!target.exists() || force && target.delete(),
        "Layout target '%s' already exists and --force was not specified", target);

    Files.move(source, target);
  }

  /**
   * Get the source file for the supplied {@code entity}.
   */
  private File getLayoutSource(Entity entity) {
    return new File(outputDir, entity.getId());
  }

  /**
   * Get the destination file for the supplied {@code entity}.
   */
  private File getLayoutTarget(Entity entity) {
    if (layout == OutputLayout.BUNDLE) {
      // "bundle/filename"
      val bundleDir = new File(outputDir, entity.getGnosId());
      val target = new File(bundleDir, entity.getFileName());

      return target;
    } else if (layout == OutputLayout.FILENAME) {
      // "filename/id"
      val fileDir = new File(outputDir, entity.getFileName());
      val target = new File(fileDir, entity.getId());

      return target;
    } else if (layout == OutputLayout.ID) {
      // "id"
      val target = new File(outputDir, entity.getId());

      return target;
    }

    throw new IllegalStateException("Unsupported layout: " + layout);
  }

  /**
   * Lookup entities by {@code objectId} from the metadata service.
   */
  private Set<Entity> resolveEntities(List<String> objectIds) {
    // Set to remove duplicates
    val entities = ImmutableSet.<Entity> builder();
    for (val objectId : objectIds) {
      val entity = metadataService.getEntity(objectId);
      entities.add(entity);

      if (index) {
        val indexEntity = metadataService.getIndexEntity(entity);
        if (indexEntity.isPresent()) {
          entities.add(indexEntity.get());
        }
      }
    }

    return entities.build();
  }

  /**
   * Filters downloadable entities.
   */
  @SneakyThrows
  private Set<Entity> filterEntities(Set<Entity> entities) {
    val filtered = ImmutableSet.<Entity> builder();
    for (val entity : entities) {
      val file = getLayoutTarget(entity);
      val skip = file.exists() && !force;
      if (skip) {
        continue;
      }

      filtered.add(entity);
    }

    return filtered.build();
  }

  @SneakyThrows
  private long getLocalAvailableSpace() {
    val path = Paths.get(outputDir.getAbsolutePath());
    val fs = java.nio.file.Files.getFileStore(path);
    return fs.getUsableSpace();
  }

  private boolean verifyLocalAvailableSpace(Set<Entity> entities) {
    val spaceRequired = downloadService.getSpaceRequired(entities);
    val spaceAvailable = getLocalAvailableSpace();
    log.warn("space required: {} ({})  space available: {} ({})",
        formatBytes(spaceRequired), spaceRequired, formatBytes(spaceAvailable), spaceAvailable);

    if (spaceRequired > spaceAvailable) {
      terminal.printWarn("Insufficient space to download requested files: Require %s. %s Available",
          formatBytes(spaceRequired), formatBytes(spaceAvailable));
      terminal.clearLine();
      return false;
    }

    return true;
  }
}
