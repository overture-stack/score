package bio.overture.score.client.manifest;

public interface ManifestService {

  String getManifestContent(ManifestResource resource);

  DownloadManifest getDownloadManifest(ManifestResource resource);

  UploadManifest getUploadManifest(ManifestResource resource);
}
