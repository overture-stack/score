package bio.overture.score.client.manifest;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileEntity {
  private boolean controlledAccess;
  private Set<ParticipantEntity> participants;
  private String fileId;
  private String fileName;
  private String dataType;
  private String fileFormat;
  private long size;
  private String latestDid;
  private String id;
}
