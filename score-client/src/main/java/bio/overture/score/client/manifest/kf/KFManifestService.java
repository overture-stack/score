package bio.overture.score.client.manifest.kf;

import bio.overture.score.client.manifest.DownloadManifest;
import bio.overture.score.client.manifest.DownloadManifest.ManifestEntry;
import bio.overture.score.client.manifest.ManifestResource;
import bio.overture.score.client.manifest.ManifestService;
import bio.overture.score.client.manifest.UploadManifest;
import bio.overture.score.client.manifest.kf.KFParser.KFEntity;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Set;

import static java.lang.String.format;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

@Service
@Profile("gen3")
public class KFManifestService implements ManifestService {

  private final KFPortalClient kfPortalClient;

  @Autowired
  public KFManifestService(KFPortalClient kfPortalClient) {
    this.kfPortalClient = kfPortalClient;
  }

  @Override
  public String getManifestContent(ManifestResource resource) {
    return getDownloadManifest(resource).toString();
  }

  @Override
  public DownloadManifest getDownloadManifest(ManifestResource resource) {
    if (resource.getType() == ManifestResource.Type.ID){
      val entities = kfPortalClient.findEntitiesFromManifest(resource.getValue());
      return convertToDownloadManifest(entities);
    } else if (resource.getType() == ManifestResource.Type.FILE){
      return null;// temproary, fill in dummy download manifest with the minimum amount of information to do a download. If have time, use KFPortalClient.findEntity(String latest_did) to create a DownloadManifest that contains one entry;
    } else {
      throw new IllegalStateException(
          format("The ManifestResource type '%s' is not supported", resource.getType()));
    }
  }

  @Override
  public UploadManifest getUploadManifest(ManifestResource resource) {
    throw new IllegalStateException("Not implemented for Gen3 mode");
  }

  private DownloadManifest convertToDownloadManifest(Set<KFEntity> entities){
   val manifestEntries =  entities.stream()
        .map(this::convertToManifestEntry)
        .collect(toImmutableList());
    return new DownloadManifest(manifestEntries);
  }

  private ManifestEntry convertToManifestEntry(KFEntity entity){
    return entity.getParticipants().stream()
        .findFirst()
        .map(x -> ManifestEntry.builder()
            .donorId(x.getParticipantId())
            .projectId(x.getStudyId()))
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
