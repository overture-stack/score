package bio.overture.score.client.manifest;

import lombok.SneakyThrows;

public interface ManifestService {

  @SneakyThrows String getManifestContent(ManifestResource resource);

  @SneakyThrows DownloadManifest getDownloadManifest(ManifestResource resource);

  @SneakyThrows UploadManifest getUploadManifest(ManifestResource resource);
}
