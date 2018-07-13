package bio.overture.score.client.manifest.kf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KFEntity {
  private boolean controlledAccess;
  private Set<KFParticipant> participants;
  private String fileId;
  private String fileName;
  private String dataType;
  private String fileFormat;
  private long size;
  private String latestDid;
  private String id;
}
