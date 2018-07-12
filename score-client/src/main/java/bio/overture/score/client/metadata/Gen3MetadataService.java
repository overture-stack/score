package bio.overture.score.client.metadata;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Profile("gen3")
public class Gen3MetadataService implements MetadataService {
  private static final List<Entity> EMPTY_LIST = ImmutableList.<Entity>of();

  @Override public List<Entity> getEntities(String... fields) {
    return EMPTY_LIST;
  }

  @Override public Entity getEntity(String objectId) {
    log.warn("The 'getEntity' method in '{}' is not correctly implemented. This is just temporary to get Gen3 downloading", getClass().getCanonicalName());
    return Entity.builder()
        .gnosId(null)
        .access(null)
        .createdTime(System.currentTimeMillis())
        .fileName(objectId)
        .id(objectId)
        .projectCode(null)
        .build();
  }

  @Override public Optional<Entity> getIndexEntity(Entity entity) {
    return Optional.empty();
  }

}
