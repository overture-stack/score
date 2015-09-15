package org.icgc.dcc.storage.client;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import lombok.val;

public class ClientMainTest extends AbstractClientMainTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testMainEmptyFile() throws Exception {
    val file = tmp.newFile();
    executeMain("upload", "--file", file.getCanonicalPath());

    assertTrue(getExitCode() == 1);
    assertTrue(getOutput().contains("Uploads of empty files are not permitted"));
  }

}
