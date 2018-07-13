package bio.overture.score.client.manifest;

import bio.overture.score.client.manifest.KFParser.KFEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import static bio.overture.score.client.manifest.KFParser.readEntries;
import static com.google.common.base.Preconditions.checkState;
import static org.springframework.http.HttpMethod.POST;

@Component
public class KFPortalClient {

  private final static String MANIFEST_ID_SEARCH_QUERY_TEMPLATE_NAME = "manifest_id_query";
  private final static String OBJECT_ID_SEARCH_QUERY_TEMPLATE_NAME = "object_id_search_query";
  private static final String MANIFEST_ID = "manifestId";
  private static final String OBJECT_ID= "objectId";
  private static final String OFFSET = "offset";
  private static final String FIRST= "first";
  private final String kfPortalUrl;
  private final RestTemplate serviceTemplate;
  private final RetryTemplate retry;
  private final TemplateEngine textTemplateEngine;

  @Autowired
  public KFPortalClient(
      @Value("${kfportal.url}") @NonNull String kfportalUrl,
      @Qualifier("serviceTemplate") @NonNull RestTemplate serviceTemplate,
      @NonNull RetryTemplate retry,
      @NonNull TemplateEngine textTemplateEngine) {
    this.kfPortalUrl = kfportalUrl;
    this.serviceTemplate = serviceTemplate;
    this.retry = retry;
    this.textTemplateEngine = textTemplateEngine;
  }

  @Data
  public static class KFEntityOld{
    //Access
    private String access;

    //Data Type
    private String dataType;

    //Experiment Strategy
    private String experimentStrategy;

    //External Aliquot Id
    private String externalAliquotId;

    //External Sample Id
    private String externalSampleId;

    //Family ID
    private String familyId;

    //File Format
    private String fileFormat;

    //File ID
    private String fileId;

    //File Name
    private String fileName;

    //File Size
    private long fileSize;

    //Icd Id Diagnosis
    private String icgIdDiagnosis;

    //Latest Did
    private String latestDid;

    //Participants Family Id
    private String participantsFamilyId;

    //Participants ID
    private String participantsId;

    //Proband
    private String proband;

    //Study ID
    private String studyId;

    //Study Name
    private String name;
  }

  public Set<KFEntity> findEntitiesFromManifest(String manifestId){
    val query = createManifestIdQuery(manifestId,0, 10000);
    val response = post(String.class, kfPortalUrl, query);
    return parseManifestEntityResponse(response);
  }

  public Optional<KFEntity> findEntity(@NonNull String objectId){
    val query = createObjectIdQuery(objectId);
    val response = post(String.class, kfPortalUrl, query);
    val entities = parseManifestEntityResponse(response);
    checkState(entities.size() < 2,
        "There was more than 1 (%s) result for the objectId '%s': %s",
        entities.size(), objectId, entities);
    return entities.stream().findFirst();
  }


  private String createObjectIdQuery(String objectId){
    val context = new Context();
    context.setVariable(OBJECT_ID, objectId);
    context.setVariable(OFFSET, 0);
    context.setVariable(FIRST, 1);
    return textTemplateEngine.process(OBJECT_ID_SEARCH_QUERY_TEMPLATE_NAME,context);

  }

  private String createManifestIdQuery(String manifestId, int offset, int size){
    val context = new Context();
    context.setVariable(MANIFEST_ID, manifestId);
    context.setVariable(OFFSET, offset);
    context.setVariable(FIRST, size);
    return textTemplateEngine.process(MANIFEST_ID_SEARCH_QUERY_TEMPLATE_NAME,context);
  }

  @SneakyThrows
  private Set<KFEntity> parseManifestEntityResponse(ResponseEntity<String> response){
    val mapper = new ObjectMapper();
    val root = mapper.readTree(response.getBody());
    return readEntries(root);
  }

  @SneakyThrows
  private <T, R> ResponseEntity<R> post(Class<R> responseType, String url, T body){
    return retry.execute(ctx -> serviceTemplate.exchange(new URI(url), POST, defaultEntity(body), responseType));
  }

  private static <T> HttpEntity<T> defaultEntity(T body) {
    return new HttpEntity<>(body, defaultHeaders());
  }

  private static HttpHeaders defaultHeaders() {
    val h =  new HttpHeaders();
    h.add(HttpHeaders.CONTENT_TYPE, "application/json");
    return h;
  }

}
