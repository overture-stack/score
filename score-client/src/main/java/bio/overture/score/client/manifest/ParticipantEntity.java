package bio.overture.score.client.manifest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParticipantEntity {
  private String participantId;
  private boolean proband;
  private String studyShortName;
  private String studyId;
}
