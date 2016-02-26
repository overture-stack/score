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
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.icgc.dcc.storage.client.command.ViewCommand.OutputFormat;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.util.AlphanumStringComparator;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@Slf4j
public class SamFileBuilder {

  // arbitrary limit - accounting for pathname as well
  public final static int MAX_FILENAME_LENGTH = 120;

  /**
   * Options
   */
  private boolean containedOnly = false;
  private boolean useOriginalHeader = true;
  private OutputFormat outputFormat = OutputFormat.SAM;
  private List<String> query = Lists.<String> newArrayList();
  private File outputDir;
  private boolean outputIndex = false;

  /*
   * Intermediate Objects
   */
  private Entity entity;
  private SamInputResource samInputResource;

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

  @SneakyThrows
  private SamReader createSamReader() {
    try {
      // Need to use non-STRICT due to header date formats in the wild.
      return SamReaderFactory.makeDefault().validationStringency(ValidationStringency.LENIENT).open(samInputResource);
    } catch (RuntimeIOException e) {
      log.error("Error opening SamReader: ", e);
      val rootCause = Throwables.getRootCause(e);
      if (rootCause instanceof IOException) {
        throw new RuntimeException("Error opening SAM resource: " + rootCause.getMessage(), rootCause);
      }
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
  private SAMFileWriter prepareOutput(@NonNull String fileName, @NonNull SAMFileHeader srcHeader) {
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
  private SAMFileWriter prepareOutput(@NonNull String fileName, @NonNull SAMFileHeader srcHeader,
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
  private void createHeaderOnly(SamReader reader, Entity entity) {
    val sourceHeader = reader.getFileHeader();
    val outputFileName = generateHeaderOnlyOutputFileName(entity);

    @Cleanup
    // Calls close() on writer which flushes everything to disk
    val writer = prepareOutput(outputFileName, sourceHeader);
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

    // Assemble list of sequence refs from both fields in alignment record
    SortedSet<String> sequenceRefs = getSequenceReferences(alignments);

    val outputFileName = generateOutputFileName(entity, queries);

    // Ok - now we can finally write things out
    val sourceHeader = reader.getFileHeader();

    @Cleanup
    val writer = prepareOutput(outputFileName, sourceHeader, sequenceRefs);
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
    // Go through returned alignments and find all the Mate/Next Reads (RNEXT)
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
        log.warn("Warning: Sequence Name {} not found in source BAM file", ref);
      }
    }
    return newHeader;
  }

  @SneakyThrows
  private SAMFileWriter createSamFileWriter(SAMFileHeader header, String fileName) {
    val factory = new SAMFileWriterFactory()
        .setCreateIndex(outputIndex)
        .setCreateMd5File(false)
        .setUseAsyncIo(true);

    val stdout = (fileName == null) || fileName.trim().isEmpty();

    if (stdout) {
      return outputFormat == OutputFormat.BAM ? factory.makeBAMWriter(header, true, System.out) : factory
          .makeSAMWriter(header, true, System.out);
    } else {
      val outFile = new File(fileName);

      // Overwrite existing output
      if (outFile.exists()) {
        outFile.delete();
      }

      return outputFormat == OutputFormat.BAM ? factory.makeBAMWriter(header, true, outFile) : factory
          .makeSAMWriter(header, true, outFile);
    }
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
    // Truncate everything prior to .bam or .bam.bai suffix
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

  @SneakyThrows
  public void buildTrimmed() {
    @Cleanup
    val reader = createSamReader();

    // Actually perform query - we will need alignments to determine contents of Sequence Dictionary in header
    List<SAMRecord> alignments = doQuery(reader, query);
    createTrimmed(reader, entity, query, alignments);
  }

  @SneakyThrows
  public void buildHeaderOnly() {
    @Cleanup
    val reader = createSamReader();
    createHeaderOnly(reader, entity); // use original filename and no query
  }
}
