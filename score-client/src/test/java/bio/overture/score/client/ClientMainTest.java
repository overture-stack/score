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
package bio.overture.score.client;

import bio.overture.score.client.storage.Gen3StorageService;
import bio.overture.score.core.model.Part;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
public class ClientMainTest extends AbstractClientMainTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testMainViewFileWithBadOutputType() throws Exception {
    executeMain("view", "--output-format", "xxx", "--verify-connection", "false");

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains(
        "Bad parameter(s): \"--output-format\": couldn't convert \"xxx\" to a value in [bam, sam, cram]"));
  }

  @Test
  public void testRr(){
    val url = "https://kf-study-us-east-1-prd-sd-bhjxbdqk.s3.amazonaws.com/source/fastq/RNA_468928_7316-1778.fq.gz?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIZAHWHQWDB7XV4RA%2F20180711%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20180711T214803Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&user_id=93&username=RTISMA&X-Amz-Signature=16f782cdc50a87abafbddd1f930c929f96133d7563eac50d82b53d33fb18dddf";
    val offset = 0;
    val length = 30*1024*1024*1024L;
    val part = Part.builder()
        .partNumber(0)
        .partSize(length)
        .offset(offset)
        .url(url)
        .build();
    val md5 = Gen3StorageService.getPartMd5(part);
    log.info("sdf");

  }
  @Test
  public void testRob() throws Exception {
    executeMain("--profile",  "gen3", "info");

    assertTrue(getExitCode() == 0);

//    assertTrue(getOutput().contains("One of --object-id, --input-file or --manifest must be specified"));
  }

  @Test
  public void testMainViewFileWithUpperCaseOutputFormat() throws Exception {
    val outDir = tmp.newFolder();
    executeMain("view", "--output-format", "BAM", "--output-dir", outDir.getCanonicalPath(), "--verify-connection",
        "false");

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("One of --object-id, --input-file or --manifest must be specified"));
  }

  @Test
  public void testMainUploadMissingFileArgument() throws Exception {
    executeMain("upload", "--object-id", UUID.randomUUID().toString(), "--verify-connection", "false");

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("--file must be specified if --object-id is specified"));
  }

  @Test
  public void testMainUploadFileButMissingMd5() throws Exception {
    val file = tmp.newFile();
    executeMain("upload", "--object-id", UUID.randomUUID().toString(), "--file", file.getCanonicalPath(),
        "--verify-connection", "false");

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("--md5 must be specified if --object-id is specified"));
  }

  @Test
  public void testMainDownloadWithNonExistentManifest() throws Exception {
    val file = new File("/foo");
    val outDir = tmp.newFolder();
    executeMain("download", "--manifest", file.getCanonicalPath(), "--output-dir", outDir.getCanonicalPath(),
        "--verify-connection", "false");

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("Could not read manifest from 'file:/foo': /foo (No such file or directory)"));
  }

  @Test
  public void testMainDownloadWithNonExistentOutDir() throws Exception {
    val file = tmp.newFile();
    // create our own directory placeholder in temporary folder
    val outDir = new File(tmp.getRoot(), "foo");
    assertFalse(outDir.exists());

    executeMain("download", "--manifest", file.getCanonicalPath(), "--output-dir", outDir.getCanonicalPath(),
        "--verify-connection", "false");

    assertTrue(outDir.exists());

    // will fail on empty manifest, not on missing output dir
    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains(" is empty"));
  }

  @Test
  public void testMainDownloadWithEmptyManifest() throws Exception {
    val file = tmp.newFile();
    val outDir = tmp.newFolder();
    executeMain("download", "--manifest", file.getCanonicalPath(), "--output-dir", outDir.getCanonicalPath(),
        "--verify-connection", "false");

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains(" is empty"));
  }

  @Ignore
  @Test
  public void testMainDownloadWithPopulatedManifest() throws Exception {
    val file = new File("src/test/resources/fixtures/download/manifest.txt");
    val outDir = tmp.newFolder();
    executeMain("download", "--manifest", file.getCanonicalPath(), "--output-dir", outDir.getCanonicalPath(),
        "--verify-connection", "false");

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains(" is empty. Exiting."));
  }

  @Test
  public void testMainDownloadWithBadObjectId() throws Exception {
    val outDir = tmp.newFolder();
    executeMain("download", "--object-id", "xxx", "--out-dir", outDir.getCanonicalPath(), "--verify-connection",
        "false");

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("Invalid option: --object-id: xxx is not a valid UUID"));
  }

  @Test
  @Ignore
  public void testViewWithBadDateInHeader() throws Exception {
    // TODO: Find out why this tmp outdir isn't working
    val outDir = tmp.newFolder();
    val file = "src/test/resources/fixtures/view/94c1f438-acc8-51dd-a44e-e24d32a46c07.bam";
    executeMain("view", "--header-only", "--input-file", file, "--output-format", "sam", "--output-dir",
        outDir.getCanonicalPath(), "--verify-connection", "false");

    assertTrue(getExitCode() == 0);
  }

}
