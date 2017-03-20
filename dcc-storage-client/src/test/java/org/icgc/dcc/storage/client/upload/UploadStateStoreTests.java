package org.icgc.dcc.storage.client.upload;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import lombok.val;

import org.junit.Test;

public class UploadStateStoreTests {

  File getTestResourceRoot() {
    return new File(getClass().getClassLoader().getResource("fixtures/upload/placeholder-upload-file.txt").getFile());
  }

  @Test
  public void test_fetch_upload_id_finds_file() throws IOException {
    val dir = getTestResourceRoot();
    Optional<String> result = UploadStateStore.fetchUploadId(dir, "valid-upload-id");
    assertThat(result.get()).isEqualTo("this-is-my-uploadId");
  }

  @Test
  public void test_fetch_upload_id_finds_improperly_formatted_file() throws IOException {
    val dir = getTestResourceRoot();
    Optional<String> result = UploadStateStore.fetchUploadId(dir, "invalid-upload-id");
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void test_upload_state_store_containing_dir() throws IOException {
    val pathString = getTestResourceRoot().getCanonicalPath();
    val testFile = new File(pathString);
    val resultFile = UploadStateStore.getContainingDir(testFile);
    assertThat(resultFile).isNotNull();
  }
}
