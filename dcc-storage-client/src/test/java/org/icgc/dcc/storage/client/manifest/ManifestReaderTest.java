package org.icgc.dcc.storage.client.manifest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.icgc.dcc.storage.client.manifest.Manifest.ManifestEntry;
import org.junit.Test;

import lombok.val;

public class ManifestReaderTest {

  @Test
  public void testReadManifest() {
    val reader = new ManifestReader();
    val manifest = reader.readManifest(new File("src/test/resources/fixtures/download/manifest.txt"));

    assertThat(manifest.getEntries(), hasSize(2));
    assertThat(manifest.getEntries().get(0), equalTo(ManifestEntry.builder()
        .repoCode("1")
        .fileId("2")
        .fileUuid("3")
        .fileFormat("4")
        .fileName("5")
        .fileSize("6")
        .fileMd5sum("7")
        .indexFileUuid("8")
        .donorId("9")
        .projectId("10")
        .build()));
    assertThat(manifest.getEntries().get(1), equalTo(ManifestEntry.builder()
        .repoCode("10")
        .fileId("9")
        .fileUuid("8")
        .fileFormat("7")
        .fileName("6")
        .fileSize("5")
        .fileMd5sum("4")
        .indexFileUuid("3")
        .donorId("2")
        .projectId("1")
        .build()));

  }

}
