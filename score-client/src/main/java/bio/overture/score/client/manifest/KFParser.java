package bio.overture.score.client.manifest;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;

import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;

public class KFParser {
  private static final String DATA = "data";
  private static final String FILE= "file";
  private static final String HITS = "hits";
  private static final String EDGES = "edges";
  private static final String NODE = "node";
  private static final String KF_ID = "kf_id";
  private static final String DATA_TYPE= "data_type";
  private static final String FILE_FORMAT= "file_format";
  private static final String SIZE= "size";
  private static final String LATEST_DID= "latest_did";
  private static final String ID= "id";
  private static final String PARTICIPANTS= "participants";
  private static final String IS_PROBAND= "is_proband";
  private static final String STUDY = "study";
  private static final String SHORT_NAME= "short_name";
  private static final String CONTROLLED_ACCESS = "controlled_access";

  public static Set<KFEntity> readEntries(JsonNode root){
    val edgesNode = getEdgesNode(getHitsNode(getFileNode(getDataNode(root))));
    return stream(edgesNode)
        .map(KFParser::getNodeNode)
        .map(x -> KFEntity.builder()
            .controlledAccess(isControlledAccess(x))
            .participants(readParticipants(getParticipantsNode(x)))
            .fileId(readFileId(x))
            .dataType(readDataType(x))
            .fileFormat(readFileFormat(x))
            .size(readSize(x))
            .latestDid(readLatestDid(x))
            .id(readId(x))
            .build()
        ).collect(toImmutableSet());
  }

  private static JsonNode getDataNode(JsonNode root){
    return requiredGet(root,DATA);
  }

  private static JsonNode getFileNode(JsonNode root){
    return requiredGet(root,FILE);
  }

  private static JsonNode getHitsNode(JsonNode root){
    return requiredGet(root, HITS);
  }

  private static JsonNode getEdgesNode(JsonNode root){
    return requiredGet(root, EDGES);
  }

  private static boolean isControlledAccess(JsonNode root){
    return requiredGet(root, CONTROLLED_ACCESS).booleanValue();
  }

  private static JsonNode getNodeNode(JsonNode root){
    return requiredGet(root, NODE);
  }

  private static String readFileId(JsonNode node){
    return requiredGet(node, KF_ID).textValue();
  }

  private static String readDataType(JsonNode node){
    return requiredGet(node, DATA_TYPE).textValue();
  }

  private static String readFileFormat(JsonNode node){
    return requiredGet(node, FILE_FORMAT).textValue();
  }

  private static long readSize(JsonNode node){
    return requiredGet(node, SIZE).longValue();
  }

  private static String readLatestDid(JsonNode node){
    return requiredGet(node, LATEST_DID).textValue();
  }

  private static String readId(JsonNode node){
    return requiredGet(node, ID).textValue();
  }

  private static String readParticipantId(JsonNode node){
    return requiredGet(node, KF_ID).textValue();
  }

  private static boolean isProband(JsonNode node){
    return requiredGet(node, IS_PROBAND).booleanValue();
  }

  private static String readShortStudy(JsonNode node){
    return requiredGet(requiredGet(node, STUDY), SHORT_NAME).textValue();
  }

  private static JsonNode getParticipantsNode(JsonNode node){
    return requiredGet(node, PARTICIPANTS);
  }

  private static Set<KFParticipant> readParticipants(JsonNode participantsNode){
    val edgesNode = getEdgesNode(getHitsNode(participantsNode));
    return stream(edgesNode)
        .map(KFParser::getNodeNode)
        .map(x -> KFParticipant.builder()
            .participantId(readParticipantId(x))
            .proband(isProband(x))
            .studyShortName(readShortStudy(x))
            .build()
        )
        .collect(toImmutableSet());
  }
  private static Optional<JsonNode> optionalGet(JsonNode root, String key){
    if (!root.has(key) || root.path(key).isNull() ){
      return Optional.empty();
    }
    return Optional.of(root.path(key));
  }

  private static JsonNode requiredGet(JsonNode root, String key){
    checkArgument(root.has(key), "The following root element doesnt have the key '%s': %s", key, root);
    return root.path(key);
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class KFEntity {
    private boolean controlledAccess;
    private Set<KFParticipant> participants;
    private String fileId;
    private String dataType;
    private String fileFormat;
    private long size;
    private String latestDid;
    private String id;
  }

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class KFParticipant{
    private String participantId;
    private boolean proband;
    private String studyShortName;
  }


}
