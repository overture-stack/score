package bio.overture.score.client.metadata;

import java.util.List;
import java.util.Optional;
import lombok.NonNull;

public interface MetadataService {
  List<Entity> getEntities(String... fields);

  Entity getEntity(String objectId);

  Optional<Entity> getIndexEntity(Entity entity);

  List<String> getObjectIdsByAnalysisId(@NonNull String programId, @NonNull String analysisId);
}
