package bio.overture.score.client.metadata.kf;

import bio.overture.score.client.manifest.kf.KFFileEntity;
import bio.overture.score.client.manifest.kf.KFParticipantEntity;
import bio.overture.score.client.manifest.kf.KFPortalClient;
import bio.overture.score.client.metadata.Entity;
import bio.overture.score.client.metadata.EntityNotFoundException;
import bio.overture.score.client.metadata.MetadataService;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

@Slf4j
@Service
@Profile("kf")
public class KFMetadataService implements MetadataService {
  private static final List<Entity> EMPTY_LIST = ImmutableList.<Entity>of();
  private static final String CONTROLLED = "controlled";
  private static final String OPEN= "open";
  private static final String DEFAULT_BUNDLE = "unknown_bundle";

  private final KFPortalClient kfPortalClient;

  @Autowired
  public KFMetadataService(@NonNull KFPortalClient kfPortalClient) {
    this.kfPortalClient = kfPortalClient;
  }

  @Override public List<Entity> getEntities(String... fields) {
    return EMPTY_LIST;
  }

  @Override public Entity getEntity(String objectId) {
    val result = kfPortalClient.findEntity(objectId);
    val kfEntity = result.orElseThrow(
        () -> new EntityNotFoundException(format("The KF entity with objectId '%s' does not exist", objectId))
    );
    return Entity.builder()
        .gnosId(kfEntity.getParticipants().stream().map(KFParticipantEntity::getParticipantId).findFirst().orElse(DEFAULT_BUNDLE))
        .access(resolveAccess(kfEntity) )
        .createdTime(System.currentTimeMillis())
        .fileName(kfEntity.getFileName())
        .id(objectId)
        .projectCode(kfEntity.getParticipants().stream()
            .findFirst()
            .map(KFParticipantEntity::getStudyId)
            .orElse(null))
        .build();
  }

  private static String resolveAccess(KFFileEntity kfFileEntity){
    return kfFileEntity.isControlledAccess() ? CONTROLLED : OPEN;
  }

  @Override public Optional<Entity> getIndexEntity(Entity entity) {
    return Optional.empty();
  }

  @Override public List<String> getObjectIdsByAnalysisId(@NonNull String programId, @NonNull String analysisId) {
    throw new NotImplementedException();
  }

}
