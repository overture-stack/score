package bio.overture.score.client.metadata;

import static java.lang.String.format;

import bio.overture.score.client.manifest.FileEntity;
import bio.overture.score.client.manifest.ParticipantEntity;
import bio.overture.score.client.manifest.PortalClient;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MetadataServiceImpl implements MetadataService {
  private static final List<Entity> EMPTY_LIST = ImmutableList.<Entity>of();
  private static final String CONTROLLED = "controlled";
  private static final String OPEN = "open";
  private static final String DEFAULT_BUNDLE = "unknown_bundle";

  private final PortalClient portalClient;

  @Autowired
  public MetadataServiceImpl(@NonNull PortalClient portalClient) {
    this.portalClient = portalClient;
  }

  @Override
  public List<Entity> getEntities(String... fields) {
    return EMPTY_LIST;
  }

  @Override
  public Entity getEntity(String objectId) {
    val result = portalClient.findEntity(objectId);
    val fileEntity =
        result.orElseThrow(
            () ->
                new EntityNotFoundException(
                    format("The entity with objectId '%s' does not exist", objectId)));
    return Entity.builder()
        .gnosId(
            fileEntity.getParticipants().stream()
                .map(ParticipantEntity::getParticipantId)
                .findFirst()
                .orElse(DEFAULT_BUNDLE))
        .access(resolveAccess(fileEntity))
        .createdTime(System.currentTimeMillis())
        .fileName(fileEntity.getFileName())
        .id(objectId)
        .projectCode(
            fileEntity.getParticipants().stream()
                .findFirst()
                .map(ParticipantEntity::getStudyId)
                .orElse(null))
        .build();
  }

  private static String resolveAccess(FileEntity fileEntity) {
    return fileEntity.isControlledAccess() ? CONTROLLED : OPEN;
  }

  @Override
  public Optional<Entity> getIndexEntity(Entity entity) {
    return Optional.empty();
  }

  @Override
  public List<String> getObjectIdsByAnalysisId(
      @NonNull String programId, @NonNull String analysisId) {
    throw new NotImplementedException();
  }
}
