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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.storage.client.cli.DirectoryValidator;
import org.icgc.dcc.storage.client.cli.ObjectIdValidator;
import org.icgc.dcc.storage.client.cli.OutputLayoutConverter;
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

/**
 * Handle download command line arguments
 */
@Component
@Parameters(separators = "=", commandDescription = "Retrieve object(s) from remote storage")
public class DownloadCommand extends AbstractClientCommand {

  public enum OutputLayout {
    bundle, filename, id
  }

  /**
   * Options
   */
  @Parameter(names = "--output-dir", description = "path to output directory", required = true, validateValueWith = DirectoryValidator.class)
  private File outDir;
  @Parameter(names = "--output-layout", description = "layout of the output-dir. One of 'bundle' (nest files in bundle directory), 'filename' (nest files in filename directory), or 'id' (flat list of files named with object-id)", converter = OutputLayoutConverter.class)
  private OutputLayout layout = OutputLayout.filename;
  @Parameter(names = "--force", description = "force re-download (override local file)")
  private boolean force = false;
  @Parameter(names = "--manifest", description = "path to manifest id, url or file")
  private String manifestSpec;
  @Parameter(names = "--object-id", description = "object id to download", validateValueWith = ObjectIdValidator.class)
  private String oid;
  @Parameter(names = "--offset", description = "position in source file to begin download from")
  private long offset = 0;
  @Parameter(names = "--length", description = "the number of bytes to download (in bytes)")
  private long length = -1;
  @Parameter(names = "--index", description = "download file index if available?")
  private boolean index = true;

  /**
   * Dependencies
   */
  @Autowired
  private ManfiestService manfiestService;
  @Autowired
  private MetadataService metadataService;
  @Autowired
  private DownloadService downloadService;

  @Override
  public int execute() throws Exception {
    terminal.printStatus("Downloading...");
    if (!outDir.exists()) {
      terminal.printError("Output directory '%s' is missing. Exiting...", outDir.getCanonicalPath());
      return FAILURE_STATUS;
    }

    val single = oid != null;
    if (single) {
      // Ad-hoc single
      return downloadObjects(ImmutableList.of(oid));
    } else {
      // Manifest based
      val manifest = manfiestService.getManifest(new ManifestResource(manifestSpec));
      val entries = manifest.getEntries();
      if (entries.isEmpty()) {
        terminal.printError("Manifest '%s' is empty. Exiting...", manifest);
        return FAILURE_STATUS;
      }

      return downloadObjects(manifest.getEntries().stream().map(entry -> entry.getFileUuid()).collect(toList()));
    }
  }

  private int downloadObjects(List<String> objectIds) throws IOException {
    val entities = resolveEntities(objectIds);
    prepareLayout(entities);

    int i = 1;
    terminal.println("");
    for (val entity : filterEntities(entities)) {
      terminal
          .printLine()
          .printf("[%s/%s] Downloading object: %s (%s)%n", i++, entities.size(), terminal.value(entity.getId()),
              entity.getFileName())
          .printLine();
      downloadService.download(outDir, entity.getId(), offset, length, force);
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
          checkParameter(file.delete(), "Could not delete '%s'. Exiting...", file.getCanonicalPath());
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
    checkParameter(targetDir.exists() || targetDir.mkdirs(),
        "Could not create layout target directory %s", targetDir);

    checkParameter(!target.exists() || force && target.delete(),
        "Layout target '%s' already exists and --force was not specified.", target);

    Files.move(source, target);
  }

  /**
   * Get the sourcd file for the supplied {@code entity}.
   */
  private File getLayoutSource(Entity entity) {
    return new File(outDir, entity.getId());
  }

  /**
   * Get the destination file for the supplied {@code entity}.
   */
  private File getLayoutTarget(Entity entity) {
    if (layout == OutputLayout.bundle) {
      // "bundle/filename"
      val bundleDir = new File(outDir, entity.getGnosId());
      val target = new File(bundleDir, entity.getFileName());

      return target;
    } else if (layout == OutputLayout.filename) {
      // "filename/id"
      val fileDir = new File(outDir, entity.getFileName());
      val target = new File(fileDir, entity.getId());

      return target;
    } else if (layout == OutputLayout.id) {
      // "id"
      val target = new File(outDir, entity.getId());

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

}
