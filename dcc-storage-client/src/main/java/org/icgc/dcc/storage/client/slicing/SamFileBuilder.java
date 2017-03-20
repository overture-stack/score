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
package org.icgc.dcc.storage.client.slicing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.icgc.dcc.storage.client.command.ViewCommand.OutputFormat;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.RuntimeIOException;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SamFileBuilder {

  // Arbitrary limit - actual max for file name (not including path) is probably 255
  public final static int MAX_FILENAME_LENGTH = 128;

  /**
   * Options
   */
  private boolean containedOnly = false;
  private boolean useOriginalHeader = true;
  private OutputFormat outputFormat = OutputFormat.SAM;
  private List<String> query = Lists.<String> newArrayList();
  private File outputDir;
  private boolean outputIndex = false;
  private boolean stdout = false;
  private File bedFile;

  /**
   * Informational - for @PG record
   */
  private String programId;
  private String programName;
  private String commandLine;
  private String description;
  private String version;

  private Logger session = LoggerFactory.getLogger("session");

  /**
   * Intermediate Objects
   */
  private Entity entity;
  private SamInputResource samInputResource;

  private boolean queryCompiledFlag = false;

  public SamFileBuilder containedOnly(boolean flag) {
    containedOnly = flag;
    return this;
  }

  public SamFileBuilder useOriginalHeader(boolean flag) {
    useOriginalHeader = flag;
    return this;
  }

  public SamFileBuilder outputFormat(OutputFormat format) {
    outputFormat = format;
    return this;
  }

  public SamFileBuilder queries(List<String> queries) {
    query = queries;
    return this;
  }

  public SamFileBuilder outputDir(File dir) {
    outputDir = dir;
    return this;
  }

  public SamFileBuilder stdout(boolean flag) {
    stdout = flag;
    return this;
  }

  public SamFileBuilder bedFile(File bed) {
    bedFile = bed;
    return this;
  }

  public SamFileBuilder outputIndex(boolean flag) {
    outputIndex = flag;
    return this;
  }

  public SamFileBuilder entity(Entity e) {
    entity = e;
    return this;
  }

  public SamFileBuilder samInput(SamInputResource resource) {
    samInputResource = resource;
    return this;
  }

  public SamFileBuilder programId(String id) {
    programId = id;
    return this;
  }

  public SamFileBuilder programName(String name) {
    programName = name;
    return this;
  }

  public SamFileBuilder commandLine(String cl) {
    commandLine = cl;
    return this;
  }

  public SamFileBuilder description(String desc) {
    description = desc;
    return this;
  }

  public SamFileBuilder version(String ver) {
    version = ver;
    return this;
  }

  protected void validate() {
    if (!stdout && outputDir == null) {
      throw new IllegalStateException("Missing output directory");
    }

    if (entity == null) {
      throw new IllegalStateException("Missing entity description");
    }

    if (samInputResource == null) {
      throw new IllegalStateException("Missing Input Resource");
    }
  }

  @SneakyThrows
  protected SamReader createSamReader() {
    try {
      // Need to use non-STRICT due to header date formats in the wild.
      return SamReaderFactory.makeDefault().validationStringency(ValidationStringency.LENIENT).open(samInputResource);
    } catch (RuntimeIOException e) {
      log.error("Error opening SamReader: ", e);
      val rootCause = Throwables.getRootCause(e);
      if (rootCause instanceof IOException) {
        session.info("Error opening Reader: " + rootCause.getMessage());
        throw new RuntimeException("Error opening SAM resource: " + rootCause.getMessage(), rootCause);
      }
      session.info("Error opening Reader: " + e.getMessage());
      throw e;
    }
  }

  /**
   * Constructs writer for output of SAM/BAM file.
   * @param fileName Output file name
   * @param srcHeader SAM/BAM header from source file
   * @return Writer for output
   */
  @SneakyThrows
  private SAMFileWriter prepareHeaderOutput(@NonNull String fileName, @NonNull SAMFileHeader srcHeader) {
    if (stdout) {
      session.info("Preparing to write header only to stdout");
    } else {
      session.info(String.format("Preparing to write header only to %s ", fileName));
    }
    return createSamFileWriter(srcHeader, fileName);
  }

  /**
   * Constructs writer for output of SAM/BAM file.
   * @param fileName
   * @param srcHeader
   * @param readGroups
   * @return
   */
  @SneakyThrows
  private SAMFileWriter prepareOutput(@NonNull String fileName, @NonNull SAMFileHeader srcHeader,
      Set<SAMReadGroupRecord> readGroups) {

    if (useOriginalHeader) {
      if (stdout) {
        session.info("Preparing to write to stdout using original header");
      } else {
        session.info("Preparing to write to {} using original header", fileName);
      }

      return createSamFileWriter(srcHeader, fileName);
    }

    if (stdout) {
      session.info("Preparing to write to stdout");
    } else {
      session.info("Preparing to write to {}", fileName);
    }

    // Create subset of header that contains only sequences referred to
    val outputHeader = createNewHeader(srcHeader);
    outputHeader.setReadGroups(Lists.<SAMReadGroupRecord> newArrayList(readGroups));
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
    session.info("Adding {}", fileName);
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
  private void createHeaderOnly(SamReader reader, Entity entity) {
    val sourceHeader = reader.getFileHeader();
    val outputFileName = generateHeaderOnlyOutputFileName(entity);

    @Cleanup
    // Calls close() on writer which flushes everything to disk
    val writer = prepareHeaderOutput(outputFileName, sourceHeader);
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
  private void createTrimmed(SamReader reader, Entity entity, List<String> queries, List<SAMRecord> alignments) {
    validate();

    val outputFileName = bedFile == null ? generateOutputFileName(entity, queries) : generateOutputFileName(entity);

    // Ok - now we can finally write things out
    val sourceHeader = reader.getFileHeader();

    val readGroups = Sets.<SAMReadGroupRecord> newHashSet();
    alignments.stream().forEach(e -> readGroups.add(e.getReadGroup()));

    @Cleanup
    val writer = prepareOutput(outputFileName, sourceHeader, readGroups);
    alignments.stream().forEach(e -> writer.addAlignment(e));
  }

  /**
   * Temporarily changed to public
   * 
   * @param reader
   * @param queries
   * @return
   */
  public QueryInterval[] normalizeQueries(SamReader reader, List<String> queries) {
    val sourceHeader = reader.getFileHeader();

    session.info("  {} regions specified in query", queries.size());
    log.trace("Slicing: ");
    for (val q : queries) {
      if (!queryCompiledFlag) {
        session.info("  {}", q);
      }
      log.trace("  {}", q);
    }

    val slices = QueryHandler.parseQueryStrings(queries);
    session.info("  Parsed {} slices", slices.size());

    val intervals = QueryHandler.convertSlices(sourceHeader, slices);
    session.info("  Merged abutting and overlapping intervals into {} slices", intervals.length);

    for (QueryInterval i : intervals) {
      if (!queryCompiledFlag) {
        session.info("  {}:{}-{}", sourceHeader.getSequence(i.referenceIndex).getSequenceName(), i.start, i.end);
      }
    }
    queryCompiledFlag = true;
    return intervals;
  }

  /**
   * Temporarily changed to public
   * 
   * Returns list of SAMRecord alignments that satisfy the specified list of queries. Refers to the <b>containedOnly</b>
   * member variable.
   * @param reader Instance of the SAM Reader open to source SAM/BAM file
   * @param intervals Sorted list of queries to run
   * @return Ordered list of alignment records that satisfy the specified queries
   */
  public List<SAMRecord> doQuery(SamReader reader, QueryInterval[] intervals) {
    val alignments = Lists.<SAMRecord> newArrayList();
    val iterator = reader.query(intervals, containedOnly);
    while (iterator.hasNext()) {
      val record = iterator.next();
      alignments.add(record);
    }
    iterator.close();

    session.info("    Query returned {} alignments (entirely contained = {})", alignments.size(), containedOnly);
    return alignments;
  }

  /**
   * Constructs a new SAM File header that copies over all @SQ and @PG fields from source.
   * @param header - header of <i>source</i> SAM/BAM file
   * @return instance of a new SAM/BAM file header
   */
  private SAMFileHeader createNewHeader(@NonNull SAMFileHeader header) {
    val newHeader = new SAMFileHeader();

    val pg = updateProgramRecords(header.getProgramRecords());
    newHeader.setProgramRecords(pg);

    newHeader.setSequenceDictionary(header.getSequenceDictionary());
    return newHeader;
  }

  private List<SAMProgramRecord> updateProgramRecords(List<SAMProgramRecord> pgRecords) {
    val newPgRecords = new ArrayList<SAMProgramRecord>(pgRecords); // get returns an unmodifiable collection

    String id = programId == null ? "unknown" : programId;
    val count = getIcgcProgramRecordCount(pgRecords);
    if (count > 0) {
      id = String.format("%s.%d", id, count + 1);
    }
    SAMProgramRecord icgcClientRecord = new SAMProgramRecord(id);
    if (programName != null) icgcClientRecord.setProgramName(programName);
    if (commandLine != null) icgcClientRecord.setCommandLine(commandLine);
    if (description != null) icgcClientRecord.setAttribute("DS", description);
    if (version != null) icgcClientRecord.setAttribute("VN", version);
    if (commandLine != null) icgcClientRecord.setCommandLine(commandLine);
    newPgRecords.add(icgcClientRecord);
    return newPgRecords;
  }

  private int getIcgcProgramRecordCount(List<SAMProgramRecord> pgRecords) {
    int count = 0;
    for (val pg : pgRecords) {
      if (pg.getId().substring(0, programId.length() - 1).equalsIgnoreCase(programId)) {
        count += 1;
      }
    }
    return count;
  }

  @SneakyThrows
  private SAMFileWriter createSamFileWriter(SAMFileHeader header, String fileName) {
    if (stdout) {
      outputIndex = false;
    }

    val factory = new SAMFileWriterFactory()
        .setCreateIndex(outputIndex)
        .setCreateMd5File(false)
        .setUseAsyncIo(true);

    if (stdout) {
      return factory.makeSAMWriter(header, true, System.out);
    } else {
      val outFile = new File(fileName);
      log.debug("writing: {}", outFile.getCanonicalPath());

      // Overwrite existing output
      if (outFile.exists()) {
        if (outFile.delete()) {
          session.info("Overwriting previous output");
        } else {
          // Throws IOException if delete() fails
        }
      }

      return outputFormat == OutputFormat.BAM ? factory.makeBAMWriter(header, true, outFile) : factory
          .makeSAMWriter(header, true, outFile);
    }
  }

  String getExtension(OutputFormat fmt) {
    return String.format(".%s", fmt.toString().toLowerCase());
  }

  /**
   * Determine whether source file is a BAM or SAM based on file extension
   * @param ent - Entity containing source filename
   * @return OutputFormat enum indicating file type
   */
  OutputFormat getSourceType(Entity ent) {
    if (ent.getFileName().contains(getExtension(OutputFormat.BAM))) {
      return OutputFormat.BAM;
    } else if (ent.getFileName().contains(getExtension(OutputFormat.SAM))) {
      return OutputFormat.SAM;
    } else {
      val msg = String.format("%s could not be identified as SAM or BAM", ent.getFileName());
      throw new IllegalArgumentException(msg);
    }
  }

  /**
   * Perform string manipulation to insert modifier text to end of filename, but before file extension
   * @param entity
   * @return
   */
  private String injectFileNameModifier(Entity entity, String modifier) {
    val fname = entity.getFileName();
    val srcExtension = getExtension(getSourceType(entity));
    val extensionPos = fname.toLowerCase().indexOf(srcExtension);
    val left = StringUtils.left(fname, extensionPos);

    // get extension based on specified output format
    val newExtension = getExtension(outputFormat);
    return String.format("%s.%s%s", left, modifier, newExtension);
  }

  /**
   * Construct output file name when query is specified in a BED file
   * @param entity - provides base filename to amend
   * @return output file name
   */
  String generateOutputFileName(@NonNull Entity entity) {
    return addOutputPath(injectFileNameModifier(entity, bedFile.getName()));
  }

  /**
   * Construct output file name.
   * @param entity - provides base filename to amend
   * @param query - a single query to embed in output file name
   * @return output file name
   */
  String generateOutputFileName(@NonNull Entity entity, String query) {
    return addOutputPath(injectFileNameModifier(entity, encodeQuery(query)));
  }

  /**
   * Construct output file name.
   * @param entity - provides base filename to amend
   * @param queries - ordered list of queries to embed in output file name
   * @return output file name
   */
  String generateOutputFileName(@NonNull Entity entity, List<String> queries) {
    val bob = new StringBuilder();
    boolean firstValue = true;
    for (val q : queries) {
      if (!firstValue) {
        bob.append("__");
      }
      bob.append(encodeQuery(q));
      firstValue = false;
    }
    val fullName =
        handleMaxFilenameLength(injectFileNameModifier(entity, bob.toString()), getExtension(getSourceType(entity)));
    return addOutputPath(fullName);
  }

  /**
   * Construct output file name. Embeds indicator that this is only the header; no alignments included in output.
   * @param entity - provides base filename to amend
   * @return output file name
   */
  String generateHeaderOnlyOutputFileName(@NonNull Entity entity) {
    return addOutputPath(injectFileNameModifier(entity, "header"));
  }

  /**
   * In case of a large number of queries, we truncate the output filename if it is longer than MAX_FILENAME_LENGTH
   * characters long.
   * 
   * @param originalName - file name including all query slices
   * @return file name truncated at MAX_FILENAME_LENGTH but retaining extension. Includes '~' to indicate that it has
   * been truncated
   */
  String handleMaxFilenameLength(String originalName, String srcExtension) {
    // Truncate everything prior to supplied suffix
    if (originalName.length() <= MAX_FILENAME_LENGTH) {
      return originalName;
    }

    val extensionPos = originalName.toLowerCase().indexOf(srcExtension);
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

  @SneakyThrows
  public void buildHeaderOnly() {
    @Cleanup
    val reader = createSamReader();
    createHeaderOnly(reader, entity); // use original filename and no query
  }

  @SneakyThrows
  public void buildTrimmed() {
    session.info("----- Constructing Trimmed Output for {}", entity.getFileName());
    @Cleanup
    val reader = createSamReader();
    QueryInterval[] intervals = normalizeQueries(reader, query);

    val alignments = doQuery(reader, intervals);
    createTrimmed(reader, entity, query, alignments);
  }

  @SneakyThrows
  public void buildMerged() {
    session.info("----- Constructing Merged Output");
    @Cleanup
    val reader = createSamReader();
    QueryInterval[] intervals = normalizeQueries(reader, query);

    for (QueryInterval q : intervals) {
      doQuery(reader, Collections.singletonList(q).toArray(new QueryInterval[1]));
    }
  }

  @Override
  public String toString() {
    return "SamFileBuilder [containedOnly=" + containedOnly + ", useOriginalHeader=" + useOriginalHeader
        + ", outputFormat=" + outputFormat + ", query=" + query + ", outputDir=" + outputDir + ", outputIndex="
        + outputIndex + ", bedFile=" + bedFile + ", session=" + session + ", entity=" + entity + ", samInputResource="
        + samInputResource + ", queryCompiledFlag=" + queryCompiledFlag + "]";
  }

}
