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
import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.RuntimeIOException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
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
import org.icgc.dcc.storage.client.slicing.QueryHandler;
import org.icgc.dcc.storage.client.transport.NullSourceSeekableHTTPStream;
import org.icgc.dcc.storage.client.util.AlphanumStringComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Slf4j
@Component
@Parameters(separators = "=", commandDescription = "Locally store/display some or all of a remote SAM/BAM file object")
public class ViewCommand extends AbstractClientCommand {

  public enum OutputFormat {
    BAM, SAM
  }

  public enum OutputType {
    TRIMMED, MINI, AGGREGATE, CROSS
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
  @Parameter(names = "--output-original-header", description = "Use original header in its entirety in output", arity = 1)
  private boolean useOriginalHeader = true;
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
  @Parameter(names = "--output-dir", description = "Path to output directory. Only used with --manifest", required = true, validateValueWith = DirectoryValidator.class)
  private File outputDir;

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
    checkParameter(objectId != null || bamFile != null || manifestResource != null,
        "One of --object-id, --input-file or --manifest must be specified");

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

    @Cleanup
    val reader = createSamReader(entity);

    // Actually perform query - we will need alignments to determine contents of Sequence Dictionary in header
    List<SAMRecord> alignments = doQuery(reader, query);

    // Have to have header before creating writer
    if (headerOnly) {
      outputHeaderOnly(reader, entity.get()); // use original filename and no query
    } else {
      outputTrimmed(reader, entity.get(), query, alignments);
    }

    return SUCCESS_STATUS;
  }

  /**
   * Constructs writer for output of SAM/BAM file.
   * @param fileName Output file name
   * @param srcHeader SAM/BAM header from source file
   * @return Writer for output
   */
  @SneakyThrows
  private SAMFileWriter setupOutput(@NonNull String fileName, @NonNull SAMFileHeader srcHeader) {
    return createSamFileWriter(srcHeader, fileName);
  }

  /**
   * Constructs writer for output of SAM/BAM file. The Reference Sequence Dictionary in the new file will only contain
   * the specified list of sequence ref names.
   * @param fileName
   * @param srcHeader
   * @param sequenceRefNames
   * @return
   */
  @SneakyThrows
  private SAMFileWriter setupOutput(@NonNull String fileName, @NonNull SAMFileHeader srcHeader,
      @NonNull Set<String> sequenceRefNames) {

    if (useOriginalHeader) {
      return createSamFileWriter(srcHeader, fileName);
    }

    // Create subset of header that contains only sequences referred to
    val outputHeader = createNewHeader(srcHeader, sequenceRefNames);
    outputHeader.setSortOrder(SortOrder.coordinate);

    // Create writer using the new header
    return createSamFileWriter(outputHeader, fileName);
  }

  /**
   * Appends output path to output file name.
   * @param fileName File name
   * @return file name with output path prepended
   */
  private String addOutputPath(String fileName) {
    String outputFileName = fileName;
    if (outputDir != null) {
      outputFileName = String.format("%s%s%s", outputDir, File.separator, fileName);
    }
    return outputFileName;
  }

  /**
   * Outputs only the original header from the source SAM/BAM file.
   * @param reader
   * @param entity
   */
  @SneakyThrows
  private void outputHeaderOnly(SamReader reader, Entity entity) {
    val sourceHeader = reader.getFileHeader();
    val outputFileName = generateHeaderOnlyOutputFileName(entity);

    @Cleanup
    val writer = setupOutput(outputFileName, sourceHeader);
  }

  /**
   * Outputs SAM/BAM file containing only the supplied list of alignments. This is the "trimmed" format of output. By
   * default, the header's Reference Sequence Dictionary will only contain entries that match alignments in the file.
   * @param reader Reader initialized with source SAM/BAM file
   * @param entity Contains the source SAM/BAM file name
   * @param queries Ordered list of query strings (used to construct output file name)
   * @param alignments Ordered list of alignments to write to output SAM/BAM file
   */
  @SneakyThrows
  private void outputTrimmed(SamReader reader, Entity entity, List<String> queries, List<SAMRecord> alignments) {

    // assemble list of sequence refs from both fields in alignment record
    SortedSet<String> sequenceRefs = getSequenceReferences(alignments);

    val outputFileName = generateOutputFileName(entity, queries);

    // ok - now we can finally write things out
    val sourceHeader = reader.getFileHeader();

    @Cleanup
    val writer = setupOutput(outputFileName, sourceHeader, sequenceRefs);
    for (SAMRecord a : alignments) {
      writer.addAlignment(a);
    }
  }

  /**
   * Returns list of SAMRecord alignments that satisfy the specified list of queries. Refers to the <b>containedOnly</b>
   * member variable.
   * @param reader Instance of the SAM Reader open to source SAM/BAM file
   * @param queries Sorted list of queries to run
   * @param sourceHeader Instance of source header
   * @return Ordered list of alignment records that satisfy the specified queries
   */
  private List<SAMRecord> doQuery(SamReader reader, List<String> queries) {
    val sourceHeader = reader.getFileHeader();
    val slices = QueryHandler.parseQueryStrings(queries);
    QueryInterval[] intervals = QueryHandler.convertSlices(sourceHeader, slices);

    // Perform actual slicing
    List<SAMRecord> alignments = Lists.<SAMRecord> newArrayList();
    val iterator = reader.query(intervals, containedOnly);
    while (iterator.hasNext()) {
      val record = iterator.next();
      alignments.add(record);
    }
    return alignments;
  }

  /**
   * @param alignments
   * @return
   */
  private SortedSet<String> getSequenceReferences(List<SAMRecord> alignments) {
    // go through returned alignments and find all the Mate/Next Reads (RNEXT)
    SortedSet<String> sequenceRefs = Sets.<String> newTreeSet(new AlphanumStringComparator());
    for (SAMRecord a : alignments) {
      if (!a.getReferenceName().equals(SAMRecord.NO_ALIGNMENT_REFERENCE_NAME)) {
        sequenceRefs.add(a.getReferenceName());
      }

      if (!a.getMateReferenceName().equals(SAMRecord.NO_ALIGNMENT_REFERENCE_NAME)) {
        sequenceRefs.add(a.getMateReferenceName());
      }
    }
    return sequenceRefs;
  }

  /**
   * Constructs a new SAM File header that copies over mandatory fields from source. Copies over only fields from
   * Sequence Dictionary that are referenced in the new file.
   * @param header - header of <i>source</i> SAM/BAM file
   * @param sequenceRefs - ordered set of Reference Sequence Names required in new header
   * @return instance of a new SAM/BAM file header
   */
  private SAMFileHeader createNewHeader(@NonNull SAMFileHeader header, @NonNull Set<String> sequenceRefs) {
    SAMFileHeader newHeader = new SAMFileHeader();
    for (val pr : header.getProgramRecords()) {
      newHeader.addProgramRecord(pr);
    }
    for (String ref : sequenceRefs) {
      val sq = header.getSequence(ref);
      if (sq != null) {
        newHeader.addSequence(sq);
      } else {
        terminal.println("Warning: Sequence Name " + ref + " not found in source BAM file");
        log.warn("Warning: Sequence Name {} not found in source BAM file", ref);
      }
    }
    return newHeader;
  }

  private Optional<Entity> getEntity(String oid) {
    // if objectId is present, get gnos id associated with it
    return Optional
        .ofNullable(oid != null && !oid.trim().isEmpty() ? metadataService.getEntity(oid) : null);
  }

  @SneakyThrows
  private SamReader createSamReader(Optional<Entity> entity) {
    // Line up bam and index file (encapsulated in a SamInputResource)
    val resource = createInputResource(entity);
    try {
      // Need to use non-STRICT due to header date formats in the wild.
      return SamReaderFactory.makeDefault().validationStringency(ValidationStringency.LENIENT).open(resource);
    } catch (RuntimeIOException e) {
      log.error("Error opening SamReader: ", e);
      val rootCause = Throwables.getRootCause(e);
      if (rootCause instanceof IOException) {
        throw new RuntimeException("Error opening SAM resource: " + rootCause.getMessage(), rootCause);
      }
      throw e;
    }
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

  @SneakyThrows
  private SAMFileWriter createSamFileWriter(SAMFileHeader header, String fileName) {
    val factory = new SAMFileWriterFactory()
        .setCreateIndex(true)
        .setCreateMd5File(false)
        .setUseAsyncIo(true);

    // val stdout = (fileName == null) || fileName.trim().isEmpty();
    val outFile = new File(fileName);
    terminal.println("writer: " + outFile.getCanonicalPath());

    // get rid of existing output
    if (outFile.exists()) {
      outFile.delete();
    }

    return outputFormat == OutputFormat.BAM ? factory.makeBAMWriter(header, true, outFile) : factory
        .makeSAMWriter(header, true, outFile);
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

  /**
   * Construct output file name.
   * @param entity - provides base filename to amend
   * @param query - a single query to embed in output file name
   * @return output file name
   */
  String generateOutputFileName(@NonNull Entity entity, String query) {
    val fname = entity.getFileName();
    val extensionPos = fname.toLowerCase().indexOf(".bam");
    val left = StringUtils.left(fname, extensionPos);
    val extension = StringUtils.right(fname, fname.length() - extensionPos);
    return addOutputPath(String.format("%s.%s%s", left, encodeQuery(query), extension));
  }

  /**
   * Construct output file name.
   * @param entity - provides base filename to amend
   * @param queries - ordered list of queries to embed in output file name
   * @return output file name
   */
  String generateOutputFileName(@NonNull Entity entity, List<String> queries) {
    val fname = entity.getFileName();
    val extensionPos = fname.toLowerCase().indexOf(".bam");
    val left = StringUtils.left(fname, extensionPos);
    val extension = StringUtils.right(fname, fname.length() - extensionPos);

    StringBuilder bob = new StringBuilder();
    boolean firstValue = true;
    for (val q : queries) {
      if (!firstValue) {
        bob.append("__");
      }
      bob.append(encodeQuery(q));
      firstValue = false;
    }
    String fullName = String.format("%s.%s%s", left, bob.toString(), extension);
    return addOutputPath(handleMaxFilenameLength(fullName));
  }

  /**
   * Construct output file name. Embeds indicator that this is only the header; no alignments included in output.
   * @param entity - provides base filename to amend
   * @return output file name
   */
  String generateHeaderOnlyOutputFileName(@NonNull Entity entity) {
    val fname = entity.getFileName();
    val extensionPos = fname.toLowerCase().indexOf(".bam");
    val left = StringUtils.left(fname, extensionPos);
    val extension = StringUtils.right(fname, fname.length() - extensionPos);

    String fullName = String.format("%s.%s%s", left, "header", extension);
    return addOutputPath(fullName);
  }

  /**
   * In case of a large number of queries, we truncate the output filename if it is longer than MAX_FILENAME_LENGTH
   * characters long.
   * 
   * @param originalName - file name including all query slices
   * @return file name truncated at MAX_FILENAME_LENGTH but retaining extension. Includes '~' to indicate that it has
   * been truncated
   */
  String handleMaxFilenameLength(String originalName) {
    // truncate everything prior to .bam or .bam.bai suffix
    if (originalName.length() <= MAX_FILENAME_LENGTH) {
      return originalName;
    }

    val extensionPos = originalName.toLowerCase().indexOf(".bam");
    val left = StringUtils.left(originalName, MAX_FILENAME_LENGTH - 1);
    val extension = StringUtils.right(originalName, originalName.length() - extensionPos);

    return String.format("%s%s%s", left, "~", extension);
  }

  /**
   * Replaces colons with underscores.
   * @param query
   * @return string where colons have been replaced with underscores
   */
  private String encodeQuery(String query) {
    return query.replace(":", "_");
  }

}
