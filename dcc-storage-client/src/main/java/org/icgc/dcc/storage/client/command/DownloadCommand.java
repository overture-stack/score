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

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.icgc.dcc.storage.client.cli.DirectoryValidator;
import org.icgc.dcc.storage.client.cli.FileValidator;
import org.icgc.dcc.storage.client.cli.ObjectIdValidator;
import org.icgc.dcc.storage.client.cli.OutputLayoutConverter;
import org.icgc.dcc.storage.client.download.ObjectDownload;
import org.icgc.dcc.storage.client.manifest.ManifestReader;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.metadata.MetadataClient;
import org.icgc.dcc.storage.client.metadata.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
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
    BUNDLE, FILE_NAME, ID
  }

  @Parameter(names = { "--output-dir", "--out-dir" /* Deprecated */ }, description = "path to output directory", required = true, validateValueWith = DirectoryValidator.class)
  private File outDir;

  @Parameter(names = { "--output-layout" }, description = "layout of the output-dir", converter = OutputLayoutConverter.class)
  private OutputLayout layout = OutputLayout.FILE_NAME;

  @Parameter(names = "--force", description = "force re-download (override local file)", required = false)
  private boolean force = false;

  @Parameter(names = "--manifest", description = "path to manifest file", required = false, validateValueWith = FileValidator.class)
  private File manifestFile;

  @Parameter(names = "--object-id", description = "object id to download", required = false, validateValueWith = ObjectIdValidator.class)
  private String oid;

  @Parameter(names = "--offset", description = "position in source file to begin download from", required = false)
  private long offset = 0;

  @Parameter(names = "--length", description = "the number of bytes to download (in bytes)", required = false)
  private long length = -1;

  @Parameter(names = "--index", description = "download file index if available?", required = false)
  private boolean index = true;

  @Autowired
  private ObjectDownload downloader;

  @Autowired
  private MetadataClient metadataClient;
  @Autowired
  private MetadataService metadataService;

  @Override
  @SneakyThrows
  public int execute() {
    if (!outDir.exists()) {
      println("Output directory '%s' is missing. Exiting.", outDir.getCanonicalPath());
      return FAILURE_STATUS;
    }

    val single = oid != null;
    if (single) {
      // Ad-hoc single
      return downloadObjects(ImmutableList.of(oid));
    } else {
      // Manifest based
      val manifest = new ManifestReader().readManifest(manifestFile);
      val entries = manifest.getEntries();
      if (entries.isEmpty()) {
        println("Manifest '%s' is empty. Exiting.", manifestFile.getCanonicalPath());
        return FAILURE_STATUS;
      }

      return downloadObjects(manifest.getEntries().stream().map(entry -> entry.getFileUuid()).collect(toList()));
    }
  }

  private int downloadObjects(List<String> objectIds) throws IOException {
    val entities = resolveEntities(objectIds);

    int i = 1;
    for (val entity : entities) {
      println("[%s/%s] Downloading object: %s (%s)", i, entities.size(), entity.getId(), entity.getFileName());
      downloader.download(outDir, entity.getId(), offset, length, force);
      layout(entity);
      println("");
    }

    return SUCCESS_STATUS;
  }

  private void layout(Entity entity) throws IOException {
    val source = getLayoutSource(entity);
    val target = getLayoutTarget(entity);
    if (target.equals(source)) {
      return;
    }

    val targetDir = target.getParentFile();
    checkState(targetDir.exists() || targetDir.mkdirs(),
        "Could not create layout target directory %s", targetDir);

    checkState(!target.exists() || force && target.delete(),
        "Layout target '%s' already exists and --force was not specified.", target);

    Files.move(source, target);
  }

  private List<Entity> resolveEntities(List<String> objectIds) {
    val entities = ImmutableList.<Entity> builder();
    for (val objectId : objectIds) {
      val entity = metadataClient.findEntity(objectId);
      entities.add(entity);

      if (index) {
        val indexEntity = metadataService.getIndexEntity(entity);
        if (indexEntity.isPresent()) {
          entities.add(entity);
        }
      }
    }

    return entities.build();
  }

  private File getLayoutSource(Entity entity) {
    return new File(outDir, entity.getId());
  }

  private File getLayoutTarget(Entity entity) {
    if (layout == OutputLayout.BUNDLE) {
      // "bundle/filename"
      val bundleDir = new File(outDir, entity.getGnosId());
      val target = new File(bundleDir, entity.getFileName());

      return target;
    } else if (layout == OutputLayout.FILE_NAME) {
      // "filename/id"
      val fileDir = new File(outDir, entity.getFileName());
      val target = new File(fileDir, entity.getId());

      return target;
    } else if (layout == OutputLayout.ID) {
      // "id"
      val target = getLayoutSource(entity);

      return target;
    }

    throw new IllegalStateException("Unsupported layout: " + layout);
  }

}
