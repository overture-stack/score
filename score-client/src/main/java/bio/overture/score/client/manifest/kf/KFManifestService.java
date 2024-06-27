package bio.overture.score.client.manifest.kf;

import static bio.overture.score.core.util.Collectors.toImmutableList;
import static java.lang.String.format;

import bio.overture.score.client.manifest.DownloadManifest;
import bio.overture.score.client.manifest.DownloadManifest.ManifestEntry;
import bio.overture.score.client.manifest.ManifestResource;
import bio.overture.score.client.manifest.ManifestService;
import bio.overture.score.client.manifest.UploadManifest;
import java.io.File;
import java.util.Set;
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("kf")
public class KFManifestService implements ManifestService {

  private final KFPortalClient kfPortalClient;
  private final KFDownloadManifestReader kfDownloadManifestReader;

  @Autowired
  public KFManifestService(
      @NonNull KFPortalClient kfPortalClient,
      @NonNull KFDownloadManifestReader kfDownloadManifestReader) {
    this.kfPortalClient = kfPortalClient;
    this.kfDownloadManifestReader = kfDownloadManifestReader;
  }

  @Override
  public String getManifestContent(ManifestResource resource) {
    return getDownloadManifest(resource).toString();
  }

  @Override
  public DownloadManifest getDownloadManifest(ManifestResource resource) {
    if (resource.getType() == ManifestResource.Type.ID) {
      val entities = kfPortalClient.findEntitiesFromManifest(resource.getValue());
      return convertToDownloadManifest(entities);
    } else if (resource.getType() == ManifestResource.Type.FILE) {
      return kfDownloadManifestReader.readManifest(new File(resource.getValue()));
    } else {
      throw new IllegalStateException(
          format("The ManifestResource type '%s' is not supported", resource.getType()));
    }
  }

  @Override
  public UploadManifest getUploadManifest(ManifestResource resource) {
    throw new IllegalStateException("Not implemented for KF mode");
  }

  private DownloadManifest convertToDownloadManifest(Set<KFFileEntity> entities) {
    val manifestEntries =
        entities.stream().map(this::convertToManifestEntry).collect(toImmutableList());
    return new DownloadManifest(manifestEntries);
  }

  private ManifestEntry convertToManifestEntry(KFFileEntity entity) {
    return entity.getParticipants().stream()
        .findFirst()
        .map(x -> ManifestEntry.builder().donorId(x.getParticipantId()).projectId(x.getStudyId()))
        .orElse(ManifestEntry.builder())
        .fileFormat(entity.getFileFormat())
        .fileId(entity.getFileId())
        .fileMd5sum(null)
        .fileName(entity.getFileName())
        .fileUuid(entity.getLatestDid())
        .fileSize(Long.toString(entity.getSize()))
        .indexFileUuid(null)
        .repoCode(null)
        .study(null)
        .build();
  }
}
