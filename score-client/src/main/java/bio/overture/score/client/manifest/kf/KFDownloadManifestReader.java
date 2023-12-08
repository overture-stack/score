package bio.overture.score.client.manifest.kf;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import bio.overture.score.client.manifest.DownloadManifest;
import bio.overture.score.client.manifest.DownloadManifest.ManifestEntry;
import bio.overture.score.client.util.CsvParser;
import java.io.File;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class KFDownloadManifestReader {

  private final CsvParser<KFFileBean> kfFileBeanCsvParser;

  @Autowired
  public KFDownloadManifestReader(@NonNull CsvParser<KFFileBean> kfFileBeanCsvParser) {
    this.kfFileBeanCsvParser = kfFileBeanCsvParser;
  }

  @SneakyThrows
  public DownloadManifest readManifest(@NonNull File manifestFile) {
    val entries =
        kfFileBeanCsvParser.parseFile(manifestFile).stream()
            .map(KFDownloadManifestReader::convertToManifestEntry)
            .collect(toImmutableList());
    return new DownloadManifest(entries);
  }

  private static ManifestEntry convertToManifestEntry(KFFileBean bean) {
    return ManifestEntry.builder()
        .projectId(null)
        .donorId(bean.getParticipantsId())
        .fileFormat(bean.getFileFormat())
        .fileId(bean.getFileId())
        .fileMd5sum(null)
        .fileName(bean.getFileName())
        .fileSize(null)
        .fileUuid(bean.getLatestDid())
        .indexFileUuid(null)
        .indexFileUuid(null)
        .repoCode(null)
        .study(null)
        .build();
  }
}
