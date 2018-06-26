package bio.overture.score.server.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.web.client.RestTemplate;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class SongService {

  private static final String ANALYSIS_STATE = "analysisState";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @NonNull private final String serverUrl;

  private RestTemplate restTemplate = new RestTemplate();

  public MetadataEntity readLegacyEntity(@NonNull String id){
    return restTemplate.getForObject(getLegacyEntityUrl(id), MetadataEntity.class);
  }

  @SneakyThrows
  public String readAnalysisState(@NonNull String studyId, @NonNull String analysisId  ){
    val response = restTemplate.getForObject(getAnalysis(studyId,analysisId), String.class);
    val jsonResponse = OBJECT_MAPPER.readTree(response);
    return parseAnalysisState(jsonResponse);
  }

  private String getLegacyEntityUrl(String id){
    return format("%s/entities/%s", serverUrl, id);
  }

  private String getAnalysis(String studyId, String analysisId){
    return format("%s/studies/%s/analysis/%s", serverUrl, studyId, analysisId);
  }

  public static SongService createSongService(String serverUrl) {
    return new SongService(serverUrl);
  }

  private static String parseAnalysisState(JsonNode response){
    checkArgument(response.has(ANALYSIS_STATE),
        "Could not parse '%s' from SONG server response: %s",
        ANALYSIS_STATE, response.textValue());
    return response.path(ANALYSIS_STATE).textValue();
  }

}
