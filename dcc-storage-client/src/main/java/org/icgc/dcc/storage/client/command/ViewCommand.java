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
import static org.icgc.dcc.storage.client.cli.Parameters.checkParameter;
import htsjdk.samtools.SamInputResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.client.cli.ConverterFactory.OutputFormatConverter;
import org.icgc.dcc.storage.client.cli.ConverterFactory.OutputTypeConverter;
import org.icgc.dcc.storage.client.cli.DirectoryValidator;
import org.icgc.dcc.storage.client.cli.FileValidator;
import org.icgc.dcc.storage.client.cli.ObjectIdValidator;
import org.icgc.dcc.storage.client.download.DownloadService;
import org.icgc.dcc.storage.client.manifest.ManfiestService;
import org.icgc.dcc.storage.client.manifest.ManifestResource;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.metadata.MetadataService;
import org.icgc.dcc.storage.client.slicing.SamFileBuilder;
import org.icgc.dcc.storage.client.transport.NullSourceSeekableHTTPStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;

@Slf4j
@Component
@Parameters(separators = "=", commandDescription = "Locally store/display some or all of a remote SAM/BAM file object")
public class ViewCommand extends AbstractClientCommand {

  public enum OutputFormat {
    BAM, SAM
  }

  public enum OutputType {
    TRIMMED, MINI, CROSS
  }

  // arbitrary limit - accounting for pathname as well
  public final static int MAX_FILENAME_LENGTH = 120;

  /**
   * Options.
   */
  @Parameter(names = "--contained", description = "Output only alignments completely contained in specified region. By default, any alignment"
      + " that intersects with a specified region will be returned")
  private boolean containedOnly = false;
  @Parameter(names = "--header-only", description = "Output header of SAM/BAM file only")
  private boolean headerOnly = false;
  @Parameter(names = "--output-original-header", description = "Use original header in its entirety in output")
  private boolean useOriginalHeader = false;
  @Parameter(names = "--output-file", description = "Filename to write output to. Uses filename from metadata, or original input filename if not specified")
  private String fileName;
  @Parameter(names = "--output-format", description = "Output file format for query. SAM or BAM", converter = OutputFormatConverter.class)
  private OutputFormat outputFormat = OutputFormat.SAM;
  @Parameter(names = "--object-id", description = "Object id of BAM file to download slice from", validateValueWith = ObjectIdValidator.class)
  private String objectId;
  @Parameter(names = "--input-file", description = "Local path to BAM file. Overrides specification of --object-id", validateValueWith = FileValidator.class)
  private File bamFile = null;
  @Parameter(names = "--input-file-index", description = "Explicit local path to index file (requires --input-file)", validateValueWith = FileValidator.class)
  private File baiFile = null;
  @Parameter(names = "--query", description = "Query to define extract from BAM file (coordinate format 'sequence:start-end'). Multiple"
      + " ranges separated by space", variableArity = true)
  private List<String> query = new ArrayList<>();
  @Parameter(names = "--manifest", description = "Path to manifest id, url or file containing object id's and query ranges for batch")
  private ManifestResource manifestResource;
  @Parameter(names = "--output-type", description = "File output structure for queries. TRIMMED, MINI, AGGREGATE, CROSS. Only used with --manifest", converter = OutputTypeConverter.class)
  private OutputType outputType = OutputType.TRIMMED;
  @Parameter(names = "--output-dir", description = "Path to output directory. Only used with --manifest", validateValueWith = DirectoryValidator.class)
  private File outputDir;
  @Parameter(names = "--output-index", description = "Switch to write index files. Only used with --manifest")
  private boolean outputIndex = false;

  /**
   * Dependencies.
   */
  @Autowired
  private MetadataService metadataService;
  @Autowired
  private ManfiestService manifestService;
  @Autowired
  private DownloadService downloadService;

  @Override
  public int execute() throws Exception {
    terminal.printStatus("Viewing...");

    validateParms();

    val single = objectId != null;
    if (single) {
      // Ad-hoc single
      process(ImmutableList.of(objectId));
    } else if (manifestResource != null) {
      // Manifest based
      val manifest = manifestService.getManifest(manifestResource);
      val entries = manifest.getEntries();
      if (entries.isEmpty()) {
        terminal.printError("Manifest '%s' is empty", manifestResource);
        return FAILURE_STATUS;
      }

      process(manifest.getEntries().stream().map(entry -> entry.getFileUuid()).collect(toList()));
    }
    terminal.printStatus("Done");
    return SUCCESS_STATUS;
  }

  private void validateParms() {
    checkParameter(objectId != null || bamFile != null || manifestResource != null,
        "One of --object-id, --input-file or --manifest must be specified");

    if (objectId == null && bamFile == null) {
      checkParameter(manifestResource != null && outputDir != null,
          "--output-dir must be specified when using --manifest");
    }

    if ((manifestResource != null) && (outputIndex)) {
      // terminal.printWarn("--output-index option being ignored");
    }
  }

  @SneakyThrows
  int process(List<String> objectIds) {
    for (val objectId : objectIds) {
      if (process(objectId) != SUCCESS_STATUS) {
        // TODO: log failure
        return FAILURE_STATUS;
      }
    }
    return SUCCESS_STATUS;
  }

  /**
   * Main sequence of steps to construct a SAM/BAM file via htsjdk.
   * @param oid Object id of sample to query against
   * @return Status flag indicating whether output SAM/BAM has been written
   */
  @SneakyThrows
  int process(String oid) {

    val entity = getEntity(oid);
    if (!entity.isPresent()) {
      throw new RuntimeException("That ain't no thang. Literally");
    }

    // Line up bam and index file (encapsulated in a SamInputResource)
    val resource = createInputResource(entity);

    SamFileBuilder bob = new SamFileBuilder().
        containedOnly(containedOnly).
        useOriginalHeader(useOriginalHeader).
        outputFormat(outputFormat).
        queries(query).
        outputDir(outputDir).
        outputIndex(outputIndex).
        entity(entity.get()).
        samInput(resource);

    if (headerOnly) {
      bob.buildHeaderOnly();
    } else {
      switch (outputType) {
      case TRIMMED:
        bob.buildTrimmed();
        break;
      case MINI:
      case CROSS:
        terminal.printError("Output type '%s' not implemented", outputType.toString());
        break;
      }
    }

    return SUCCESS_STATUS;
  }

  private Optional<Entity> getEntity(String oid) {
    // if objectId is present, get gnos id associated with it
    return Optional
        .ofNullable(oid != null && !oid.trim().isEmpty() ? metadataService.getEntity(oid) : null);
  }

  private SamInputResource createInputResource(Optional<Entity> entity) {
    if (entity.isPresent()) {
      return getRemoteResource(entity.get());
    } else {
      if (baiFile == null) {
        // Use samtools convention
        baiFile = new File(bamFile.getAbsolutePath() + ".bai");
        checkParameter(baiFile.exists(), "The implied BAI file '%s' does not exist", baiFile.getAbsolutePath());
        checkParameter(baiFile.canRead(), "The implied BAI file '%s' is not readable", baiFile.getAbsolutePath());
      }
      return getFileResource(bamFile, baiFile);
    }
  }

  private SamInputResource getFileResource(File bamFile, File baiFile) {
    if (outputFormat == OutputFormat.BAM) {
      return SamInputResource.of(bamFile).index(baiFile);
    } else {
      return SamInputResource.of(bamFile);
    }
  }

  private SamInputResource getRemoteResource(Entity entity) {
    val indexEntity = metadataService.getIndexEntity(entity);
    checkParameter(indexEntity.isPresent(), "No index file associated with BAM file with object id '%s'",
        entity.getId());

    val bamFileUrl = downloadService.getUrl(entity.getId());
    val bamFileHttpStream = new NullSourceSeekableHTTPStream(bamFileUrl);
    val indexFileUrl = downloadService.getUrl(indexEntity.get().getId());
    val indexFileHttpStream = new NullSourceSeekableHTTPStream(indexFileUrl);

    return SamInputResource.of(bamFileHttpStream).index(indexFileHttpStream);
  }

}
