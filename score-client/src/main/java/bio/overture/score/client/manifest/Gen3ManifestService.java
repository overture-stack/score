package bio.overture.score.client.manifest;

import bio.overture.score.client.manifest.DownloadManifest.ManifestEntry;
import bio.overture.score.client.manifest.KFParser.KFEntity;
import bio.overture.score.client.manifest.KFParser.KFParticipant;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Set;

import static java.lang.String.format;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

@Service
@Profile("gen3")
public class Gen3ManifestService implements ManifestService{

  private final KFPortalClient kfPortalClient;

  @Autowired
  public Gen3ManifestService(KFPortalClient kfPortalClient) {
    this.kfPortalClient = kfPortalClient;
  }

  @Override
  public String getManifestContent(ManifestResource resource) {
    return getDownloadManifest(resource).toString();
  }

  @Override
  public DownloadManifest getDownloadManifest(ManifestResource resource) {
    if (resource.getType() == ManifestResource.Type.ID){
      val entities = kfPortalClient.getEntitiesFromManifest(resource.getValue());
      return convertToDownloadManifest(entities);
    } else if (resource.getType() == ManifestResource.Type.FILE){
      return null;// temproary, fill in dummy download manifest with the minimum amount of information to do a download. If have time, use KFPortalClient.getEntity(String latest_did) to create a DownloadManifest that contains one entry;
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
    return DownloadManifest.builder()
        .entries(manifestEntries)
        .build();
  }

  private ManifestEntry convertToManifestEntry(KFEntity entity){
    return ManifestEntry.builder()
        .donorId(entity.getParticipants()
            .stream()
            .findFirst()
            .map(KFParticipant::getParticipantId)
            .orElse(null))
        .fileFormat(entity.getFileFormat())
        .fileId(entity.getFileId())
        .fileMd5sum(null)
        .fileName(null) //TODO, fix the query to provide this
        .fileUuid(entity.getLatestDid())
        .fileSize(Long.toString(entity.getSize()))
        .indexFileUuid(null)
        .projectId(null ) //TODO, fix this. is this the projectCode
        .repoCode(null)
        .study(null) //TODO, fix this.shold this be PCAWG or none?
        .build();
  }

}
