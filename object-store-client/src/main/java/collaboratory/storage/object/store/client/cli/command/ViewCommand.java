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
package collaboratory.storage.object.store.client.cli.command;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import collaboratory.storage.object.store.client.download.ObjectDownload;
import collaboratory.storage.object.store.client.slicing.MetaServiceQuery;
import collaboratory.storage.object.store.client.slicing.QueryHandler;
import collaboratory.storage.object.store.client.transport.NullSourceSeekableHTTPStream;
import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReaderFactory;
import lombok.Cleanup;
import lombok.val;

@Component
@Parameters(separators = "=", commandDescription = "extract/displays some or all of SAM/BAM file")
public class ViewCommand extends AbstractClientCommand {

  @Parameter(names = "--contained", description = "output only alignments completely contained in specified region. By default, any alignment"
      + " that intersects with a specified region will be returned")
  private boolean containedOnly = false;

  @Parameter(names = "--header-only", description = "output header of SAM/BAM file only")
  private boolean headerOnly = false;

  @Parameter(names = "--output-path", description = "path to an output directory. Stdout if not specified.")
  private String filePath = "";

  @Parameter(names = "--output-file", description = "filename to write output to. Uses filename from metadata, or original input filename if not specified")
  private String fileName = "";

  @Parameter(names = "--output-type", description = "output format of query BAM/SAM. Default: BAM", validateWith = ViewOutputTypeValidator.class)
  private String outputType = "BAM"; // TODO: maybe make a String Enum for output types

  @Parameter(names = "--object-id", description = "object id of BAM file to download slice from")
  private String oid = "";

  @Parameter(names = "--input-file", description = "local path to BAM file. Overrides specification of --object-id")
  private String bamFilePath = "";

  @Parameter(names = "--input-file-index", description = "explicit local path to index file (requires --input-file)")
  private String baiFilePath = "";

  @Parameter(names = "--query", description = "query to define extract from BAM file (coordinate format 'sequence:start-end'). Multiple"
      + " ranges separated by space", variableArity = true)
  private List<String> query = new ArrayList<String>();

  @Autowired
  private MetaServiceQuery metaQuery;

  @Autowired
  private ObjectDownload downloader;

  @Override
  public int execute() {
    try {
      SamInputResource resource = createInputResource();

      @Cleanup
      val reader = SamReaderFactory.makeDefault().open(resource);
      SAMFileHeader header = reader.getFileHeader();

      QueryInterval[] intervals = QueryHandler.parseQueryStrings(header, query);

      val outputFileName = generateFileOutputName();
      @Cleanup
      val writer = createFileWriter(header, outputFileName);

      if (!headerOnly) {
        // perform actual slicing
        SAMRecordIterator iterator = reader.query(intervals, containedOnly);

        while (iterator.hasNext()) {
          val record = iterator.next();
          writer.addAlignment(record);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return SUCCESS_STATUS;
  }

  private SamInputResource createInputResource() {
    SamInputResource resource = null;
    if (!oid.trim().isEmpty()) {
      resource = getRemoteResource(oid);
    } else {
      if (bamFilePath.trim().isEmpty()) {
        throw new IllegalArgumentException("No BAM file input specified");
      } else if (baiFilePath.trim().isEmpty()) {
        resource = getFileResource(bamFilePath);
      } else {
        resource = getFileResource(bamFilePath, baiFilePath);
      }
    }
    return resource;
  }

  private SAMFileWriter createFileWriter(SAMFileHeader header, String path) {
    boolean stdout = (path == null) || path.trim().isEmpty();

    final SAMFileWriterFactory factory = new SAMFileWriterFactory();
    factory.setCreateIndex(true).setUseAsyncIo(true).setCreateMd5File(false);

    SAMFileWriter result = null;
    if (outputType.toUpperCase().equals("BAM")) {
      result = stdout ? factory.makeBAMWriter(header, true, System.out) : factory.makeBAMWriter(header, true,
          new File(path));
    } else if (outputType.toUpperCase().equals("SAM")) {
      result = stdout ? factory.makeSAMWriter(header, true, System.out) : factory.makeSAMWriter(header, true,
          new File(path));
    }
    return result;
  }

  private String generateFileOutputName() {
    String result = ""; // stdout
    // check for explicit path + filename
    if (!filePath.trim().isEmpty()) {
      if (fileName.trim().isEmpty()) {
        // generated name depends on whether user has specified object id or local bam file name
        result = generateDefaultFilename();
      } else {
        // use supplied filename
        result = filePath + File.separator + fileName;
      }
    }
    return result;
  }

  private String generateDefaultFilename() {
    String result = "";
    if (bamFilePath.trim().isEmpty()) {
      result = filePath + File.separator + metaQuery.getFileName();
    } else {
      // use filename of input file
      String fileName = new File(bamFilePath).getName();
      result = filePath + File.separator + "extract-" + fileName;
    }
    return result;
  }

  /*
   * Resource factory methods I tried refactoring them out into static factory methods but they had too many
   * dependencies (MetaServiceQuery, ObjectDownload)
   */

  /**
   * Assumes that the .BAI index file has the same name as the bamFilePath parameter: <sam/bam file name>.bam.bai
   * 
   */
  private SamInputResource getFileResource(String bamFilePath) {
    return getFileResource(bamFilePath, bamFilePath + ".bai");
  }

  private SamInputResource getFileResource(String bamFilePath, String baiFilePath) {
    File bam = new File(bamFilePath);
    File bai = new File(baiFilePath);

    if (!bam.exists()) {
      throw new IllegalArgumentException("Expected " + bamFilePath + " does not exist");
    }

    if (!bai.exists()) {
      throw new IllegalArgumentException("Expected " + baiFilePath
          + " not found. Consider setting filename with --input-file-index option");
    }

    val resource = SamInputResource.of(bam).index(bai);
    return resource;
  }

  private SamInputResource getRemoteResource(String objectId) {
    metaQuery.setObjectId(objectId);

    URL bamFileUrl = downloader.getUrl(objectId, 0, -1);

    Optional<String> indexFileId = metaQuery.getAssociatedIndexObjectId();
    checkState(indexFileId.isPresent(), "No index file associated with BAM file (object_id = %s)", objectId);

    URL indexFileUrl = downloader.getUrl(indexFileId.get());

    NullSourceSeekableHTTPStream bamFileHttpStream = new NullSourceSeekableHTTPStream(bamFileUrl);
    NullSourceSeekableHTTPStream indexFileHttpStream = new NullSourceSeekableHTTPStream(indexFileUrl);

    SamInputResource resource = SamInputResource.of(bamFileHttpStream).index(indexFileHttpStream);
    return resource;
  }
}
