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

import bio.overture.score.client.metadata.Entity;
import bio.overture.score.client.slicing.SamFileBuilder;
import bio.overture.score.client.transport.NullSourceSeekableHTTPStream;
import bio.overture.score.client.view.Viewer;
import htsjdk.samtools.*;
import htsjdk.samtools.cram.ref.ReferenceSource;
import htsjdk.samtools.seekablestream.SeekableStream;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class CramSlicingTest {
  @Before
  public void setUp() {
    // integration test
    // 1) Run minio.sh
    // 2) Run the test below
    // 3) Profit?
  }

  @Test
  public void test_cram_slicing() throws IOException {
    val endpoint="http://localhost:9444";
    val credentialsFile="../score-client/src/test/resources/s3.credentials";
    val filename="yeast.cram";
    val indexFilename="yeast.cram.crai";
    val referenceFilename="yeast.fasta";
    val dir = ".";
    val bucket="/cram";
    val query = l("X:1-10");

    // Use Minio to mock the S3 backend, and to directly generate
    // pre-signed urls for us.
    //
    // We can then directly test our CRAM slicing code against the S3
    // API, and ensure that we handle the same input files in the same
    // way that we handle local files.

    val generator = new PresignedURLGenerator(endpoint, credentialsFile);
    val cramUrl = generator.generateUrl(bucket,filename);

    // CRAM files have an index file called CRAI files
    val craiUrl = generator.generateUrl(bucket,indexFilename);
    //System.err.printf("Cram='%s', Crai='%s'\n",cramUrl,craiUrl);
    val e = new Entity();
    e.setFileName(filename);

    // Since CRAM is a compressed form of BAM file that saves space by only recording
    // the differences between an individual BAM file and a standard reference file.
    // we need to have the reference file in order to decode the CRAM file.
    val reference = new File(dir,referenceFilename);
    val v = new Viewer(reference);
    val isCram = true;

    val builder = v.getBuilder(Viewer.openInputStream(cramUrl), Viewer.openIndexStream(craiUrl), isCram)
          .queries(query).entity(e).stdout(true);

    builder.buildTrimmed();
  }

  public List<String> l(String... s) {
    return Arrays.asList(s);
  }

}
