package org.icgc.dcc.storage.client;

import static org.junit.Assert.assertTrue;

import java.io.File;

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
    assertTrue(getOutput().contains("Invalid value for --output-type parameter. Allowed values:[bam, sam]"));
  }

  @Test
  public void testMainUploadEmptyFile() throws Exception {
    val file = tmp.newFile();
    executeMain("upload", "--file", file.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("Uploads of empty files are not permitted"));
  }

  @Test
  public void testMainDownloadWithNonExistentManifest() throws Exception {
    val file = new File("/foo");
    val outDir = tmp.newFile();
    executeMain("download", "--manifest", file.getCanonicalPath(), "--out-dir", outDir.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("Bad parameter(s): Invalid option: --manifest: /foo does not exist"));
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
    executeMain("download", "--manifest", file.getCanonicalPath(), "--out-dir", outDir.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains(" is empty. Exiting."));
  }

  @Ignore
  @Test
  public void testMainDownloadWithPopulatedManifest() throws Exception {
    val file = new File("src/test/resources/fixtures/download/manifest.txt");
    val outDir = tmp.newFolder();
    executeMain("download", "--manifest", file.getCanonicalPath(), "--out-dir", outDir.getCanonicalPath());

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

}
