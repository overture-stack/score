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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.icgc.dcc.storage.client.cli.ObjectIdValidator;
import org.icgc.dcc.storage.client.cli.OutputTypeConverter;
import org.icgc.dcc.storage.client.download.DownloadService;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.metadata.MetadataService;
import org.icgc.dcc.storage.client.slicing.QueryHandler;
import org.icgc.dcc.storage.client.transport.NullSourceSeekableHTTPStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

@Component
@Parameters(separators = "=", commandDescription = "Extract/displays some or all of SAM/BAM file")
public class ViewCommand extends AbstractClientCommand {

  public enum OutputType {
    bam, sam
  }

  /**
   * Options.
   */
  @Parameter(names = "--contained", description = "output only alignments completely contained in specified region. By default, any alignment"
      + " that intersects with a specified region will be returned")
  private boolean containedOnly = false;
  @Parameter(names = "--header-only", description = "output header of SAM/BAM file only")
  private boolean headerOnly = false;
  @Parameter(names = "--output-file", description = "filename to write output to. Uses filename from metadata, or original input filename if not specified")
  private String fileName = "";
  @Parameter(names = "--output-type", description = "output format of query.", converter = OutputTypeConverter.class)
  private OutputType outputType = OutputType.sam;
  @Parameter(names = "--object-id", description = "object id of BAM file to download slice from", validateValueWith = ObjectIdValidator.class)
  private String oid;
  @Parameter(names = "--input-file", description = "local path to BAM file. Overrides specification of --object-id")
  private String bamFilePath = "";
  @Parameter(names = "--input-file-index", description = "explicit local path to index file (requires --input-file)")
  private String baiFilePath = "";
  @Parameter(names = "--query", description = "query to define extract from BAM file (coordinate format 'sequence:start-end'). Multiple"
      + " ranges separated by space", variableArity = true)
  private List<String> query = new ArrayList<String>();

  /**
   * Dependencies.
   */
  @Autowired
  private MetadataService metadataService;
  @Autowired
  private DownloadService downloadService;

  @Override
  public int execute() {
    try {
      terminal.println("\rViewing...   ");
      val entity = getEntity();
      val resource = createInputResource(entity);

      @Cleanup
      val reader = createSamReader(resource);
      val header = reader.getFileHeader();

      @Cleanup
      val writer = createSamFileWriter(header, fileName);

      if (!headerOnly) {
        // Perform actual slicing
        QueryInterval[] intervals = QueryHandler.parseQueryStrings(header, query);
        val iterator = reader.query(intervals, containedOnly);

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

  private Optional<Entity> getEntity() {
    return Optional.ofNullable(oid != null && !oid.trim().isEmpty() ? metadataService.getEntity(oid) : null);
  }

  private SamReader createSamReader(SamInputResource resource) {
    // Need to use non-STRICT due to header date formats in the wild.
    return SamReaderFactory.makeDefault().validationStringency(ValidationStringency.LENIENT).open(resource);
  }

  private SamInputResource createInputResource(Optional<Entity> entity) {
    SamInputResource resource = null;
    if (entity.isPresent()) {
      resource = getRemoteResource(entity.get());
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

  @SneakyThrows
  private SAMFileWriter createSamFileWriter(SAMFileHeader header, String path) {
    val stdout = (path == null) || path.trim().isEmpty();
    val factory = new SAMFileWriterFactory()
        .setCreateIndex(false)
        .setCreateMd5File(false)
        .setUseAsyncIo(true);

    val outputStream = stdout ? System.out : new FileOutputStream(new File(path));

    return outputType == OutputType.bam ? factory.makeBAMWriter(header, true, outputStream) : factory
        .makeSAMWriter(header, true, outputStream);
  }

  /**
   * Assumes that the .BAI index file has the same name as the bamFilePath parameter: <sam/bam file name>.bam.bai
   * 
   */
  private SamInputResource getFileResource(String bamFilePath) {
    return getFileResource(bamFilePath, bamFilePath + ".bai");
  }

  private SamInputResource getFileResource(String bamFilePath, String baiFilePath) {
    val bam = new File(bamFilePath);
    checkParameter(bam.exists(), "Input BAM file '%s' not found", bamFilePath);

    if (outputType == OutputType.bam) {
      val bai = new File(baiFilePath);
      checkParameter(bai.exists(),
          "Input BAI file '%s' not found. Consider setting filename with --input-file-index option", baiFilePath);

      return SamInputResource.of(bam).index(bai);
    } else {
      return SamInputResource.of(bam);
    }
  }

  private SamInputResource getRemoteResource(Entity entity) {
    val bamFileUrl = downloadService.getUrl(entity.getId(), 0, -1);

    val indexEntity = metadataService.getIndexEntity(entity);
    checkParameter(indexEntity.isPresent(), "No index file associated with BAM file (object_id = %s)", entity);

    val indexFileUrl = downloadService.getUrl(indexEntity.get().getId());

    val bamFileHttpStream = new NullSourceSeekableHTTPStream(bamFileUrl);
    val indexFileHttpStream = new NullSourceSeekableHTTPStream(indexFileUrl);

    return SamInputResource.of(bamFileHttpStream).index(indexFileHttpStream);
  }

}
