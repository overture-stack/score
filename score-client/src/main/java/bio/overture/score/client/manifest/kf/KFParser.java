package bio.overture.score.client.manifest.kf;

import static bio.overture.score.client.util.Collectors.toImmutableSet;
import static bio.overture.score.client.util.Streams.stream;
import static com.google.common.base.Preconditions.checkArgument;
import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public class KFParser {

  private static final String DATA = "data";
  private static final String FILE = "file";
  private static final String HITS = "hits";
  private static final String EDGES = "edges";
  private static final String NODE = "node";
  private static final String KF_ID = "kf_id";
  private static final String DATA_TYPE = "data_type";
  private static final String FILE_FORMAT = "file_format";
  private static final String SIZE = "size";
  private static final String LATEST_DID = "latest_did";
  private static final String ID = "id";
  private static final String PARTICIPANTS = "participants";
  private static final String IS_PROBAND = "is_proband";
  private static final String STUDY = "study";
  private static final String SHORT_NAME = "short_name";
  private static final String CONTROLLED_ACCESS = "controlled_access";
  private static final String EXTERNAL_ID = "external_id";
  private static final String FILE_NAME = "file_name";
  private static final String TOTAL = "total";
  private static final Set<KFFileEntity> EMPTY_KF_ENTITY_SET = ImmutableSet.of();

  public static Set<KFFileEntity> readEntries(JsonNode root) {
    val fileNode = getFileNode(getDataNode(root));
    val totalHits = readFileTotal(fileNode);
    if (totalHits > 0) {
      val edgesNode = getEdgesNode(getHitsNode(fileNode));
      return stream(edgesNode)
          .map(KFParser::getNodeNode)
          .map(
              x ->
                  KFFileEntity.builder()
                      .controlledAccess(isControlledAccess(x))
                      .participants(readParticipants(getParticipantsNode(x)))
                      .fileId(readFileId(x))
                      .fileName(readFileName(x))
                      .dataType(readDataType(x))
                      .fileFormat(readFileFormat(x))
                      .size(readSize(x))
                      .latestDid(readLatestDid(x))
                      .id(readId(x))
                      .build())
          .collect(toImmutableSet());
    } else {
      return EMPTY_KF_ENTITY_SET;
    }
  }

  private static JsonNode getDataNode(JsonNode root) {
    return requiredGet(root, DATA);
  }

  private static JsonNode getFileNode(JsonNode root) {
    return requiredGet(root, FILE);
  }

  private static JsonNode getHitsNode(JsonNode root) {
    return requiredGet(root, HITS);
  }

  private static JsonNode getHitsTotal(JsonNode hits) {
    return requiredGet(hits, TOTAL);
  }

  private static JsonNode getEdgesNode(JsonNode root) {
    return requiredGet(root, EDGES);
  }

  private static boolean isControlledAccess(JsonNode root) {
    return requiredGet(root, CONTROLLED_ACCESS).booleanValue();
  }

  private static JsonNode getNodeNode(JsonNode root) {
    return requiredGet(root, NODE);
  }

  private static String readFileId(JsonNode node) {
    return requiredGet(node, KF_ID).textValue();
  }

  private static long readFileTotal(JsonNode file) {
    return getHitsTotal(getHitsNode(file)).longValue();
  }

  private static String readFileName(JsonNode node) {
    return requiredGet(node, FILE_NAME).textValue();
  }

  private static String readDataType(JsonNode node) {
    return requiredGet(node, DATA_TYPE).textValue();
  }

  private static String readFileFormat(JsonNode node) {
    return requiredGet(node, FILE_FORMAT).textValue();
  }

  private static long readSize(JsonNode node) {
    return requiredGet(node, SIZE).longValue();
  }

  private static String readLatestDid(JsonNode node) {
    return requiredGet(node, LATEST_DID).textValue();
  }

  private static String readId(JsonNode node) {
    return requiredGet(node, ID).textValue();
  }

  private static String readParticipantId(JsonNode node) {
    return requiredGet(node, KF_ID).textValue();
  }

  private static boolean isProband(JsonNode node) {
    return requiredGet(node, IS_PROBAND).booleanValue();
  }

  private static JsonNode getStudyNode(JsonNode node) {
    return requiredGet(node, STUDY);
  }

  private static String readStudyShortName(JsonNode node) {
    return requiredGet(node, SHORT_NAME).textValue();
  }

  private static String readStudyStudyId(JsonNode node) {
    return requiredGet(node, EXTERNAL_ID).textValue();
  }

  private static JsonNode getParticipantsNode(JsonNode node) {
    return requiredGet(node, PARTICIPANTS);
  }

  private static Set<KFParticipantEntity> readParticipants(JsonNode participantsNode) {
    val edgesNode = getEdgesNode(getHitsNode(participantsNode));
    return stream(edgesNode)
        .map(KFParser::getNodeNode)
        .map(
            x ->
                KFParticipantEntity.builder()
                    .participantId(readParticipantId(x))
                    .proband(isProband(x))
                    .studyShortName(readStudyShortName(getStudyNode(x)))
                    .studyId(readStudyStudyId(getStudyNode(x)))
                    .build())
        .collect(toImmutableSet());
  }

  private static JsonNode requiredGet(JsonNode root, String key) {
    checkArgument(
        root.has(key), "The following root element doesnt have the key '%s': %s", key, root);
    return root.path(key);
  }
}
