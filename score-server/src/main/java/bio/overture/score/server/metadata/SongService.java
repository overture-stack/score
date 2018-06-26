package bio.overture.score.server.metadata;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.icgc.dcc.song.client.register.Endpoint;
import org.icgc.dcc.song.client.register.RestClient;
import org.icgc.dcc.song.server.model.analysis.AbstractAnalysis;
import org.icgc.dcc.song.server.model.enums.AnalysisStates;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.song.core.utils.JsonUtils.convertValue;
import static org.icgc.dcc.song.core.utils.JsonUtils.readTree;
import static org.icgc.dcc.song.server.model.enums.AnalysisStates.resolveAnalysisState;

@RequiredArgsConstructor(access = PRIVATE)
public class SongService {

  @NonNull private final String serverUrl;
  @NonNull private final Endpoint endpoint;

  private final RestClient restClient = new RestClient();

  public MetadataEntity readLegacyEntity(@NonNull String id){
    val status = restClient.get(getLegacyEntityUrl(id));
    return convertValue(status.getOutputs(), MetadataEntity.class);
  }

  @SneakyThrows
  public AnalysisStates readAnalysisState(@NonNull String studyId, @NonNull String analysisId  ){
    val status = restClient.get(endpoint.getAnalysis(studyId, analysisId));
    val a = convertValue(readTree(status.getOutputs()), AbstractAnalysis.class);
    return resolveAnalysisState(a.getAnalysisState());
  }


  private String getLegacyEntityUrl(String id){
    return format("%/entities/%s",serverUrl, id );
  }

  public static SongService createSongService(String serverUrl) {
    return new SongService(serverUrl, new Endpoint(serverUrl));
  }

}
