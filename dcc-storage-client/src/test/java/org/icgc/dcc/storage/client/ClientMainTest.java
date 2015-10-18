package org.icgc.dcc.storage.client;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import lombok.val;

public class ClientMainTest extends AbstractClientMainTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testMainViewFileWithBadOutputType() throws Exception {
    executeMain("view", "--output-type", "xxx");

    assertTrue(getExitCode() == 1);
    assertTrue(
        getOutput().contains("Bad parameter(s): \"--output-type\": couldn't convert \"xxx\" to a value in [bam, sam]"));
  }

  @Test
  public void testMainViewFileWithUpperCaseOutputType() throws Exception {
    executeMain("view", "--output-type", "BAM");

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("One of --object-id or --input-file must be specified"));
  }

  @Test
  public void testMainUploadEmptyFile() throws Exception {
    val file = tmp.newFile();
    executeMain("upload", "--object-id", UUID.randomUUID().toString(), "--file", file.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("Uploads of empty files are not permitted"));
  }

  @Test
  public void testMainDownloadWithNonExistentManifest() throws Exception {
    val file = new File("/foo");
    val outDir = tmp.newFolder();
    executeMain("download", "--manifest", file.getCanonicalPath(), "--output-dir", outDir.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("Command error: /foo (No such file or directory)"));
  }

  @Test
  public void testMainDownloadWithNonExistentOutDir() throws Exception {
    val file = tmp.newFile();
    val outDir = new File("/foo");
    executeMain("download", "--manifest", file.getCanonicalPath(), "--output-dir", outDir.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("Bad parameter(s): Invalid option: --output-dir: /foo does not exist"));
  }

  @Test
  public void testMainDownloadWithEmptyManifest() throws Exception {
    val file = tmp.newFile();
    val outDir = tmp.newFolder();
    executeMain("download", "--manifest", file.getCanonicalPath(), "--output-dir", outDir.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains(" is empty"));
  }

  @Ignore
  @Test
  public void testMainDownloadWithPopulatedManifest() throws Exception {
    val file = new File("src/test/resources/fixtures/download/manifest.txt");
    val outDir = tmp.newFolder();
    executeMain("download", "--manifest", file.getCanonicalPath(), "--output-dir", outDir.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains(" is empty. Exiting."));
  }

  @Test
  public void testMainDownloadWithBadObjectId() throws Exception {
    val outDir = tmp.newFolder();
    executeMain("download", "--object-id", "xxx", "--out-dir", outDir.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("Invalid option: --object-id: xxx is not a valid UUID"));
  }

  @Test
  public void testViewWithBadDateInHeader() throws Exception {
    val file = "src/test/resources/fixtures/view/94c1f438-acc8-51dd-a44e-e24d32a46c07.bam";
    executeMain("view", "--header-only", "--input-file", file, "--output-type", "sam");

    assertTrue(getExitCode() == 0);
  }

}
