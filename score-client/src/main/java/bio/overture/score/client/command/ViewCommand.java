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
package bio.overture.score.client.command;

import static bio.overture.score.client.cli.Parameters.checkParameter;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;

import bio.overture.score.client.cli.ConverterFactory.OutputFormatConverter;
import bio.overture.score.client.cli.ConverterFactory.OutputTypeConverter;
import bio.overture.score.client.cli.CreatableDirectoryValidator;
import bio.overture.score.client.cli.FileValidator;
import bio.overture.score.client.cli.ObjectIdValidator;
import bio.overture.score.client.download.DownloadService;
import bio.overture.score.client.manifest.DownloadManifest;
import bio.overture.score.client.manifest.ManifestResource;
import bio.overture.score.client.manifest.ManifestService;
import bio.overture.score.client.metadata.Entity;
import bio.overture.score.client.metadata.MetadataService;
import bio.overture.score.client.slicing.SamFileBuilder;
import bio.overture.score.client.view.Viewer;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.Feature;
import htsjdk.tribble.bed.BEDCodec;
import htsjdk.tribble.bed.BEDFeature;
import htsjdk.tribble.readers.LineIterator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.common.core.util.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Parameters(
    separators = "=",
    commandDescription = "Locally store/display some or all of a remote SAM/BAM file object")
@Profile("!kf")
public class ViewCommand extends RepositoryAccessCommand {

  public enum OutputFormat {
    BAM,
    SAM,
    CRAM
  }

  public enum OutputType {
    TRIMMED,
    MERGED,
    CROSS
  }

  // arbitrary limit - accounting for pathname as well
  public static final int MAX_FILENAME_LENGTH = 120;

  public static final String PROGRAM_NAME = "Score Client";
  public static final String ICGC = "ICGC";

  /** Options. */
  @Parameter(
      names = "--contained",
      description =
          "Output only alignments completely contained in specified region. By default, any alignment"
              + " that intersects with a specified region will be returned")
  private boolean containedOnly = false;

  @Parameter(names = "--header-only", description = "Output header of SAM/BAM file only")
  private boolean headerOnly = false;

  @Parameter(
      names = "--output-original-header",
      description = "Use original header in its entirety in output")
  private boolean useOriginalHeader = false;

  // @Parameter(names = "--output-file", description =
  // "Filename to write output to. Uses filename from metadata, or original input filename if not
  // specified")
  // private String fileName;
  @Parameter(
      names = "--output-format",
      description = "Output file format for query. SAM or BAM",
      converter = OutputFormatConverter.class)
  private OutputFormat outputFormat = OutputFormat.SAM;

  @Parameter(
      names = "--object-id",
      description = "Object id of BAM file to download slice from. Will supercede --manifest",
      validateValueWith = ObjectIdValidator.class)
  private String objectId;

  @Parameter(
      names = "--input-file",
      description = "Local path to BAM file. Will supercede specification of --object-id",
      validateValueWith = FileValidator.class)
  private File sequenceFile = null;

  @Parameter(
      names = "--input-file-index",
      description = "Explicit local path to index file (requires --input-file)",
      validateValueWith = FileValidator.class)
  private File indexFile = null;

  @Parameter(
      names = "--reference-file",
      description = "Explicit local path to the fasta file that a cram file was encoded with",
      validateValueWith = FileValidator.class)
  private File referenceFile = null;

  @Parameter(
      names = "--query",
      description =
          "Query to define extract from BAM file (coordinate format 'sequence:start-end'). Multiple"
              + " ranges separated by space",
      variableArity = true)
  private List<String> query = new ArrayList<>();

  @Parameter(
      names = "--bed-query",
      description =
          "Specify a BED-format file containing ranges to query. Overrides --query parameter",
      validateValueWith = FileValidator.class)
  private File bedFile = null;

  @Parameter(
      names = "--manifest",
      description =
          "Path to manifest id, url or file containing object id's and query ranges for batch")
  private ManifestResource manifestResource;

  @Parameter(
      names = "--output-type",
      description =
          "File output structure for queries. TRIMMED, MERGED, CROSS. Only used with --manifest",
      converter = OutputTypeConverter.class)
  private OutputType outputType = OutputType.TRIMMED;

  @Parameter(
      names = "--output-dir",
      description = "Path to output directory. Only used with --manifest",
      validateValueWith = CreatableDirectoryValidator.class)
  private File outputDir;

  @Parameter(
      names = "--output-index",
      description = "Switch to write index files. Only used with --manifest")
  private boolean outputIndex = false;

  @Parameter(
      names = "--stdout",
      description =
          "Switch to send output to stdout. Only used with --object-id. Output will be forced to SAM format.")
  private boolean stdout = false;

  @Parameter(
      names = "--verify-connection",
      description = "Verify connection to repository",
      arity = 1)
  private boolean verifyConnection = true;

  /** Dependencies. */
  @Autowired private MetadataService metadataService;

  @Autowired private ManifestService manifestService;
  @Autowired private DownloadService downloadService;
  @Autowired private ApplicationArguments applicationArguments;

  /** Session logger */
  private Logger session = LoggerFactory.getLogger("session");

  @Override
  public int execute() throws Exception {
    Viewer v;
    terminal.println("Viewing...");
    session.info("***** Beginning view session");
    validateParms();

    if (verifyConnection) {
      try {
        verifyRepoConnection();
      } catch (IOException ioe) {
        terminal.printError("Could not verify connection to Repository. " + ioe.getMessage());
      }
    }

    if (bedFile != null) {
      query = handleBedFile(bedFile);
    }

    if (sequenceFile != null) {
      v = new Viewer(referenceFile);
      val indexFileExists = !isNull(indexFile);
      val builder = configureBuilder(v.getBuilder(sequenceFile, indexFile));
      build(builder, indexFileExists);
    } else if (objectId != null) {
      // Ad-hoc single - supercedes --manifest
      if (manifestResource != null) {
        terminal.println("Ignoring --manifest argument; --object-id supercedes");
      }
      process(ImmutableList.of(objectId));
    } else if (manifestResource != null) {
      // Manifest based
      val manifest = manifestService.getDownloadManifest(manifestResource);
      val allEntries = manifest.getEntries();
      if (allEntries.isEmpty()) {
        String msg = String.format("Manifest '%s' is empty", manifestResource);
        terminal.printError(msg);
        session.info(msg);
        return FAILURE_STATUS;
      }

      val entries = filterManifest(allEntries);
      process(entries.stream().map(entry -> entry.getFileUuid()).collect(toList()));
    }
    session.info("Done");
    if (!stdout) {
      terminal.println("Done");
    }
    return SUCCESS_STATUS;
  }

  private String getCommandLine() {
    StringBuilder msg = new StringBuilder();
    for (val s : applicationArguments.getSourceArgs()) {
      msg.append(s).append(" ");
    }
    return msg.toString();
  }

  private Entity getEntity(String oid) {
    val e = fetchEntity(oid);

    if (!e.isPresent()) {
      val msg = String.format("Metadata not found for object id %s", oid);
      log.error(msg);
      session.info(msg);
      throw new RuntimeException(msg);
    }

    return e.get();
  }

  public SamFileBuilder configureBuilder(SamFileBuilder builder) {
    builder =
        builder
            .programName(PROGRAM_NAME)
            .version(VersionUtils.getScmInfo().get("git.commit.id.describe"))
            .programId(ICGC)
            .commandLine(getCommandLine())
            .containedOnly(containedOnly)
            .useOriginalHeader(useOriginalHeader)
            .outputFormat(outputFormat)
            .outputDir(outputDir)
            .outputIndex(outputIndex)
            .stdout(stdout)
            .queries(query)
            .bedFile(bedFile);
    log.info("Constructed SamFileBuilder: " + builder.toString());
    return builder;
  }

  @SneakyThrows
  int process(String oid) {
    val entity = getEntity(oid);
    val urls = getPresignedUrls(entity);

    val indexExists = !isNull(urls.index);
    val inputStream = Viewer.openInputStream(urls.file);
    val indexStream = indexExists ? Viewer.openIndexStream(urls.index) : null;
    val isCram = isCRAM(entity.getFileName());
    val viewer = new Viewer(referenceFile);

    val builder = configureBuilder(viewer.getBuilder(inputStream, indexStream, isCram));
    return build(builder.entity(entity), indexExists);
  }

  private boolean isQueryDefined() {
    return !isEmpty(bedFile) || !isNull(query) && !query.isEmpty();
  }

  @SneakyThrows
  int build(SamFileBuilder builder, boolean hasIndex) {
    if (isQueryDefined() && !hasIndex) {
      log.warn("Supplied query or bedfile will not be used since no index is available");
    }
    if (headerOnly || !hasIndex) {
      builder.buildHeaderOnly();
    } else {
      switch (outputType) {
        case TRIMMED:
          builder.buildTrimmed();
          break;
        case MERGED:
          terminal.printError("Output type '%s' not implemented", outputType.toString());
          break;
        case CROSS:
          terminal.printError("Output type '%s' not implemented", outputType.toString());
          break;
      }
    }
    return SUCCESS_STATUS;
  }

  @SneakyThrows
  int process(List<String> objectIds) {
    for (val objectId : objectIds) {
      if (process(objectId) != SUCCESS_STATUS) {
        log.error("Failed to process {}", objectId);
        return FAILURE_STATUS;
      }
    }
    return SUCCESS_STATUS;
  }

  private Optional<Entity> fetchEntity(String oid) {
    // fetch the entity for the given object id from our metadata service
    return Optional.ofNullable(
        oid != null && !oid.trim().isEmpty() ? metadataService.getEntity(oid) : null);
  }

  public PresignedUrls getPresignedUrls(Entity entity) {
    val indexEntity = metadataService.getIndexEntity(entity);
    val bamFileUrl = downloadService.getUrl(entity.getId());
    val indexFileUrl = indexEntity.map(x -> downloadService.getUrl(x.getId())).orElse(null);

    return new PresignedUrls(bamFileUrl, indexFileUrl);
  }

  private void validateParms() {
    if (stdout) {
      checkParameter(
          manifestResource == null && objectId != null && !objectId.isEmpty(),
          "Output to stdout only permitted with --object-id. Not compatible with --manifest.");

      if (outputFormat.equals(ViewCommand.OutputFormat.BAM)) {
        terminal.println("When --stdout specified, output format forced to SAM.");
      }
    }

    checkParameter(
        objectId != null || sequenceFile != null || manifestResource != null,
        "One of --object-id, --input-file or --manifest must be specified. "
            + "For CRAM files, please additionally include --reference-file.");

    if (objectId == null && sequenceFile == null) {
      checkParameter(
          manifestResource != null && outputDir != null,
          "--output-dir must be specified when using --manifest");
    }

    if (outputDir == null) {
      stdout = true;
    }
  }

  @SneakyThrows
  private List<String> handleBedFile(File bedFile) {
    val query = new ArrayList<String>();
    query.clear();
    final BEDCodec codec = new BEDCodec();
    final AbstractFeatureReader<BEDFeature, LineIterator> bfs =
        AbstractFeatureReader.getFeatureReader(bedFile.getAbsolutePath(), codec, false);
    for (final Feature feat : bfs.iterator()) {
      val region = String.format("%s:%d-%d", feat.getContig(), feat.getStart(), feat.getEnd());
      System.out.println(region);
      query.add(region);
    }
    return query;
  }

  private List<DownloadManifest.ManifestEntry> filterManifest(
      List<DownloadManifest.ManifestEntry> entries) {
    val result = Lists.<DownloadManifest.ManifestEntry>newArrayList();
    // we're only going to process CRAM/BAM/SAM files
    for (DownloadManifest.ManifestEntry me : entries) {
      if (me.isBAM() || me.isSAM() || me.isCRAM()) {
        result.add(me);
        val msg = String.format("Queuing %s", me.getFileName());
        log.info(msg);
        session.info(msg);
      } else {
        val msg = String.format("Skipping manifest file entry: %s", me.getFileName());
        log.warn(String.format(msg));
        session.info(msg);
      }
    }
    return result;
  }

  public static boolean isCRAM(String filename) {
    return filename.toLowerCase().endsWith(ViewCommand.OutputFormat.CRAM.toString().toLowerCase());
  }
}
