package bio.overture.score.client.manifest.kf;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KFFileEntity {
  private boolean controlledAccess;
  private Set<KFParticipantEntity> participants;
  private String fileId;
  private String fileName;
  private String dataType;
  private String fileFormat;
  private long size;
  private String latestDid;
  private String id;
}
