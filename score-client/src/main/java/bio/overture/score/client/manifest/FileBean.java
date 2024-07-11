package bio.overture.score.client.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonPropertyOrder({
  "File ID",
  "Latest Did",
  "File Name",
  "Data Type",
  "File Format",
  "Experiment Strategy",
  "Participants ID",
  "Proband",
  "Family ID",
  "External Sample Id",
  "External Aliquot Id"
})
public class FileBean {

  @JsonProperty("File ID")
  private String fileId;

  @JsonProperty("Latest Did")
  private String latestDid;

  @JsonProperty("File Name")
  private String fileName;

  @JsonProperty("Data Type")
  private String dataType;

  @JsonProperty("File Format")
  private String fileFormat;

  @JsonProperty("Experiment Strategy")
  private String experimentStrategy;

  @JsonProperty("Participants ID")
  private String participantsId;

  @JsonProperty("Proband")
  private String proband;

  @JsonProperty("Family ID")
  private String familyId;

  @JsonProperty("External Sample Id")
  private String externalSampleId;

  @JsonProperty("External Aliquot Id")
  private String externalAliquotId;
}
