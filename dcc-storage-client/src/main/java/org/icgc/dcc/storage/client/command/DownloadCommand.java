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

import java.io.File;
import java.io.IOException;

import org.icgc.dcc.storage.client.cli.DirectoryValidator;
import org.icgc.dcc.storage.client.cli.FileValidator;
import org.icgc.dcc.storage.client.cli.ObjectIdValidator;
import org.icgc.dcc.storage.client.cli.OutputLayoutConverter;
import org.icgc.dcc.storage.client.download.ObjectDownload;
import org.icgc.dcc.storage.client.manifest.ManifestReader;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.metadata.EntityNotFoundException;
import org.icgc.dcc.storage.client.metadata.MetadataClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
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
    BUNDLE_DIR, ID_DIR, ID_FLAT
  }

  @Parameter(names = { "--output-dir", "--out-dir" /* Deprecated */ }, description = "path to output directory", required = true, validateValueWith = DirectoryValidator.class)
  private File outDir;

  @Parameter(names = { "--output-layout" }, description = "layout of the output-dir", converter = OutputLayoutConverter.class)
  private OutputLayout layout = OutputLayout.ID_DIR;

  @Parameter(names = "--force", description = "force re-download (override local file)", required = false)
  private boolean isForce = false;

  @Parameter(names = "--manifest", description = "path to manifest file", required = false, validateValueWith = FileValidator.class)
  private File manifestFile;

  @Parameter(names = "--object-id", description = "object id to download", required = false, validateValueWith = ObjectIdValidator.class)
  private String oid;

  @Parameter(names = "--offset", description = "position in source file to begin download from", required = false)
  private long offset = 0;

  @Parameter(names = "--length", description = "the number of bytes to download (in bytes)", required = false)
  private long length = -1;

  @Autowired
  private ObjectDownload downloader;

  @Autowired
  private MetadataClient metadataClient;

  @Override
  @SneakyThrows
  public int execute() {
    if (!outDir.exists()) {
      println("Output directory '%s' is missing. Exiting.", outDir.getCanonicalPath());
      return FAILURE_STATUS;
    }

    val single = oid != null;
    if (single) {
      return downloadObject();
    } else {
      return downloadObjects();
    }
  }

  private int downloadObject() throws IOException {
    // Inline single
    Entity entity;
    try {
      entity = metadataClient.findEntity(oid);
    } catch (EntityNotFoundException e) {
      println("Entity with id '%s' does not exist. Exiting.", oid);
      return FAILURE_STATUS;
    }

    println("Downloading object: %s (%s)", oid, entity.getFileName());

    downloader.download(outDir, oid, offset, length, isForce);
    layout(entity);

    return SUCCESS_STATUS;
  }

  private int downloadObjects() throws IOException {
    // Manifest based
    val manifest = new ManifestReader().readManifest(manifestFile);
    val entries = manifest.getEntries();
    if (entries.isEmpty()) {
      println("Manifest '%s' is empty. Exiting.", manifestFile.getCanonicalPath());
      return FAILURE_STATUS;
    }

    int i = 1;
    for (val entry : entries) {
      val objectId = entry.getFileUuid();

      Entity entity;
      try {
        entity = metadataClient.findEntity(objectId);
      } catch (EntityNotFoundException e) {
        println("Entity with id '%s' is empty. Exiting.", objectId);
        return FAILURE_STATUS;
      }

      println("[%s/%s] Downloading object: %s (%s)", i++, entries.size(), objectId, entity.getFileName());
      downloader.download(outDir, entry.getFileUuid(), offset, length, isForce);
      layout(entity);
      println("");
    }

    return SUCCESS_STATUS;
  }

  private void layout(Entity entity) throws IOException {
    if (layout == OutputLayout.BUNDLE_DIR) {
      val bundleDir = new File(outDir, entity.getGnosId());
      if (!bundleDir.exists()) {
        checkState(bundleDir.mkdir(), "Could not create directory %s", bundleDir);
      }

      Files.move(new File(outDir, entity.getId()), new File(bundleDir, entity.getFileName()));
    } else if (layout == OutputLayout.ID_DIR) {
      val fileDir = new File(outDir, entity.getId());
      if (!fileDir.exists()) {
        checkState(fileDir.mkdir(), "Could not create directory %s", fileDir);
      }

      Files.move(new File(outDir, entity.getId()), new File(fileDir, entity.getFileName()));
    }
  }

}
