package bio.overture.score.server.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MetadataFile {
  private String objectId;
  private String studyId;
  private String analysisId;
  private String fileName;
  private String fileSize;
  private String fileType;
  private String fileMd5sum;
  private String fileAccess;
}
