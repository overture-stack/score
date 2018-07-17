package bio.overture.score.client.manifest.kf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KFParticipantEntity {
  private String participantId;
  private boolean proband;
  private String studyShortName;
  private String studyId;
}
