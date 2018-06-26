package bio.overture.score.server.metadata;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.icgc.dcc.song.client.register.Endpoint;
import org.icgc.dcc.song.client.register.RestClient;
import org.icgc.dcc.song.server.model.analysis.AbstractAnalysis;
import org.icgc.dcc.song.server.model.enums.AnalysisStates;

import static java.lang.String.format;
import static org.icgc.dcc.song.core.utils.JsonUtils.convertValue;
import static org.icgc.dcc.song.server.model.enums.AnalysisStates.resolveAnalysisState;

@RequiredArgsConstructor
public class SongService {

  private final String serverUrl;
  private final Endpoint endpoint;
  private final RestClient restClient = new RestClient();

  public MetadataEntity readLegacyEntity(@NonNull String id){
    val status = restClient.get(getLegacyEntityUrl(id));
    return convertValue(status.getOutputs(), MetadataEntity.class);
  }

  public AnalysisStates readAnalysisState(@NonNull MetadataEntity metadataEntity){
    return readAnalysisState(
        getStudyId(metadataEntity),
        getAnalysisId(metadataEntity));
  }

  public AnalysisStates readAnalysisState(@NonNull String studyId, @NonNull String analysisId  ){
    val status = restClient.get(endpoint.getAnalysis(studyId, analysisId));
    val a = convertValue(status.getOutputs(), AbstractAnalysis.class);
    return resolveAnalysisState(a.getAnalysisState());
  }

  private String getLegacyEntityUrl(String id){
    return format("%/entities/%s",serverUrl, id );
  }

  private static String getStudyId(MetadataEntity metadataEntity){
    return metadataEntity.getProjectCode();
  }

  private static String getAnalysisId(MetadataEntity metadataEntity){
    return metadataEntity.getGnosId();
  }

}
