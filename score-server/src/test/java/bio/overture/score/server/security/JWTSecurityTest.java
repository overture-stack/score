package bio.overture.score.server.security;

import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.server.config.SecurityConfig;
import bio.overture.score.server.metadata.MetadataEntity;
import bio.overture.score.server.metadata.MetadataService;
import bio.overture.score.server.repository.DownloadService;
import bio.overture.score.server.repository.UploadService;
import bio.overture.score.server.utils.JWTGenerator;
import bio.overture.score.server.utils.JwtContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.KeyPair;
import java.util.List;
import java.util.Map;

import static bio.overture.score.server.security.JWTSecurityTest.RequestType.DOWNLOAD;
import static bio.overture.score.server.security.JWTSecurityTest.RequestType.UPLOAD;
import static bio.overture.score.server.security.JWTSecurityTest.ScopeOptions.*;
import static bio.overture.score.server.utils.JwtContext.buildJwtContext;
import static java.util.Objects.isNull;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@Slf4j
@SpringBootTest
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"test", "secure", "jwt", "default", "dev"})
public class JWTSecurityTest {

    // -- constants --
    private final static String CONTROLLED = "controlled";
    private final static String PUBLISHED = "PUBLISHED";

    private final static String NON_EXISTING_PROJECT_CODE = "FAKE";

    private final static String EXISTING_PROJECT_CODE = "TEST";
    private final static String EXISTING_OBJECT_ID = "123";
    private final static String EXISTING_GNOS_ID = "123";

    private final static String DOWNLOAD_ENDPOINT = "/download/" + EXISTING_OBJECT_ID + "?offset=0&length=-1";
    private final static String UPLOAD_ENDPOINT = "/upload/" + EXISTING_OBJECT_ID + "/uploads?fileSize=1";

    private final static boolean EXPIRED = true;
    private final static boolean NOT_EXPIRED = !EXPIRED;

    // -- Dependencies --
    @MockBean
    private JwtDecoder jwtDecoder;
    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private SecurityConfig securityConfig;
    @Autowired private KeyPair keyPair;

    // -- Mocking --
    private MockMvc mockMvc;
    @MockBean private MetadataService metadataService;
    @MockBean private DownloadService downloadService;
    @MockBean private UploadService uploadService;

    private JWTGenerator jwtGenerator;
    private Map<ScopeOptions, List<String>> downloadScopesMap;
    private Map<ScopeOptions, List<String>> uploadScopesMap;

    @Before
    @SneakyThrows
    public void beforeEachTest() {
        jwtGenerator = new JWTGenerator(keyPair);
        if (mockMvc == null) {
            this.mockMvc =
                    MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();

            downloadScopesMap = Map.of(
                    VALID_SYSTEM,   List.of(resolveSystemDownloadScope(), "id.READ"),
                    VALID_STUDY,    List.of(resolveStudyDownloadScope(EXISTING_PROJECT_CODE), "id.READ"),
                    INVALID_STUDY,  List.of(resolveStudyDownloadScope(NON_EXISTING_PROJECT_CODE), "id.READ")
            );

            uploadScopesMap = Map.of(
                    VALID_SYSTEM, List.of(resolveSystemUploadScope(), "id.WRITE"),
                    VALID_STUDY, List.of(resolveStudyUploadScope(EXISTING_PROJECT_CODE), "id.WRITE"),
                    INVALID_STUDY, List.of(resolveStudyUploadScope(NON_EXISTING_PROJECT_CODE), "id.WRITE")
            );
        }
        setMocks();
    }

    @Test public void jwtDownloadValidation_validStudyScope_Success() {
        executeAndAssert(DOWNLOAD, VALID_STUDY, NOT_EXPIRED, OK);
    }
    @Test public void jwtDownloadValidation_validSystemScope_Success() {
        executeAndAssert(DOWNLOAD, VALID_SYSTEM, NOT_EXPIRED, OK);
    }
    @Test public void jwtDownloadValidation_validStudyScopeExpired_Unauthorized() {
        executeAndAssert(DOWNLOAD, VALID_STUDY, EXPIRED, UNAUTHORIZED);
    }
    @Test public void jwtDownloadValidation_validSystemScopeExpired_Unauthorized() {
        executeAndAssert(DOWNLOAD, VALID_SYSTEM, EXPIRED, UNAUTHORIZED);
    }
    @Test public void jwtDownloadValidation_missingScopeField_Forbidden() {
        executeAndAssert(DOWNLOAD, EMPTY_SCOPE, NOT_EXPIRED, FORBIDDEN);
    }
    @Test public void jwtDownloadValidation_missingScopeFieldExpired_Unauthorized() {
        executeAndAssert(DOWNLOAD, EMPTY_SCOPE, EXPIRED, UNAUTHORIZED);
    }
    @Test public void jwtDownloadValidation_invalidSystemScope_Forbidden() {
        executeAndAssert(DOWNLOAD, INVALID_SYSTEM, NOT_EXPIRED, FORBIDDEN);
    }
    @Test public void jwtDownloadValidation_invalidStudyScope_Forbidden() {
        executeAndAssert(DOWNLOAD, INVALID_STUDY, NOT_EXPIRED, FORBIDDEN);
    }
    @Test public void jwtDownloadValidation_invalidSystemScopeExpired_Unauthorized() {
        executeAndAssert(DOWNLOAD, INVALID_SYSTEM, EXPIRED, UNAUTHORIZED);
    }
    @Test public void jwtDownloadValidation_invalidStudyScopeExpired_Unauthorized() {
        executeAndAssert(DOWNLOAD, INVALID_STUDY, EXPIRED, UNAUTHORIZED);
    }
    @Test public void jwtDownloadValidation_malformedAccessToken_Unauthorized() {
        executeAndAssert(DOWNLOAD, MALFORMED, NOT_EXPIRED, UNAUTHORIZED);
    }

    @Test public void jwtUploadValidation_validStudyScope_Success() {
        executeAndAssert(UPLOAD, VALID_STUDY, NOT_EXPIRED, OK);
    }
    @Test public void jwtUploadValidation_validSystemScope_Success() {
        executeAndAssert(UPLOAD, VALID_SYSTEM, NOT_EXPIRED, OK);
    }
    @Test public void jwtUploadValidation_validStudyScopeExpired_Unauthorized() {
        executeAndAssert(UPLOAD, VALID_STUDY, EXPIRED, UNAUTHORIZED);
    }
    @Test public void jwtUploadValidation_validSystemScopeExpired_Unauthorized() {
        executeAndAssert(UPLOAD, VALID_SYSTEM, EXPIRED, UNAUTHORIZED);
    }
    @Test public void jwtUploadValidation_missingScopeField_Forbidden() {
        executeAndAssert(UPLOAD, EMPTY_SCOPE, NOT_EXPIRED, FORBIDDEN);
    }
    @Test public void jwtUploadValidation_missingScopeFieldExpired_Unauthorized() {
        executeAndAssert(UPLOAD, EMPTY_SCOPE, EXPIRED, UNAUTHORIZED);
    }
    @Test public void jwtUploadValidation_invalidSystemScope_Forbidden() {
        executeAndAssert(UPLOAD, INVALID_SYSTEM, NOT_EXPIRED, FORBIDDEN);
    }
    @Test public void jwtUploadValidation_invalidStudyScope_Forbidden() {
        executeAndAssert(UPLOAD, INVALID_STUDY, NOT_EXPIRED, FORBIDDEN);
    }
    @Test public void jwtUploadValidation_invalidSystemScopeExpired_Unauthorized() {
        executeAndAssert(UPLOAD, INVALID_SYSTEM, EXPIRED, UNAUTHORIZED);
    }
    @Test public void jwtUploadValidation_invalidStudyScopeExpired_Unauthorized() {
        executeAndAssert(UPLOAD, INVALID_STUDY, EXPIRED, UNAUTHORIZED);
    }
    @Test  public void jwtUploadValidation_malformedAccessToken_Unauthorized() {
        executeAndAssert(UPLOAD, MALFORMED, NOT_EXPIRED, UNAUTHORIZED);
    }

    private void executeAndAssert(
            RequestType requestType, ScopeOptions scopeOptions, boolean expired, HttpStatus statusToAssert) {
        val res = getResponseFromRequestWithJwt(requestType, scopeOptions, expired);
        assertEquals(statusToAssert, res.getStatusCode());
    }

    private  ResponseEntity<String> getResponseFromRequestWithJwt(
            RequestType requestType, ScopeOptions scopeOptions, boolean expired) {
        val jwtString = generateConstrainedJWTString(scopeOptions, requestType, expired);

        val headers = createHeaderWithJwt(jwtString);

        ResponseEntity<String> res = null;
        if (requestType == RequestType.DOWNLOAD) {
            res =  executeRequest(HttpMethod.GET, DOWNLOAD_ENDPOINT, headers);
        } else if (requestType == UPLOAD) {
            res =  executeRequest(HttpMethod.POST, UPLOAD_ENDPOINT, headers);
        } else {
            fail("shouldn't be here");
        }
        return res;
    }

    @SneakyThrows
    private ResponseEntity<String> executeRequest(HttpMethod httpMethod, String url, HttpHeaders headers) {
        val mvcRequest = MockMvcRequestBuilders.request(httpMethod, url).headers(headers);
        mvcRequest.content("");
        val mvcResult = mockMvc.perform(mvcRequest).andReturn();
        val mvcResponse = mvcResult.getResponse();
        val httpStatus = HttpStatus.resolve(mvcResponse.getStatus());
        String responseObject;
        assert httpStatus != null;
        if (httpStatus.isError()) {
            responseObject = mvcResponse.getContentAsString();
            if (isBlank(responseObject) && !isNull(mvcResult.getResolvedException())) {
                responseObject = mvcResult.getResolvedException().getMessage();
            }
        } else {
            responseObject = mvcResponse.getContentAsString();
        }
        return ResponseEntity.status(mvcResponse.getStatus()).body(responseObject);
    }

    private String generateConstrainedJWTString(
            ScopeOptions scopeOptions, RequestType requestType, boolean expired) {
        if (scopeOptions == ScopeOptions.MALFORMED) { return ""; }

        JwtContext context = null;
        if (scopeOptions == ScopeOptions.INVALID_SYSTEM) {
            context = buildJwtContext(List.of("song.READ"));
        }  else if (scopeOptions == ScopeOptions.EMPTY_SCOPE) {
            context = buildJwtContext(List.of());
        } else if (requestType == DOWNLOAD) {
            context = buildJwtContext(downloadScopesMap.get(scopeOptions));
        } else if (requestType == UPLOAD) {
            context = buildJwtContext(uploadScopesMap.get(scopeOptions));
        } else {
            fail("shouldn't be here");
        }

        String jwtString = null;
        if (isNull(context)) {
            jwtString = jwtGenerator.generateJwtNoContext(expired);
        } else {
            jwtString = jwtGenerator.generateJwtWithContext(context, expired);
        }
        return jwtString;
    }

    private String resolveSystemDownloadScope() {
        return securityConfig.getScopeProperties().getDownload().getSystem();
    }

    private String resolveSystemUploadScope() {
        return securityConfig.getScopeProperties().getUpload().getSystem();
    }

    private String resolveStudyDownloadScope(String studyId) {
        val studyDownloadScope = securityConfig.getScopeProperties().getDownload().getStudy();
        return studyDownloadScope.getPrefix() + studyId + studyDownloadScope.getSuffix();
    }

    private String resolveStudyUploadScope(String studyId) {
        val studyUploadScope = securityConfig.getScopeProperties().getUpload().getStudy();
        return studyUploadScope.getPrefix() + studyId + studyUploadScope.getSuffix();
    }

    private HttpHeaders createHeaderWithJwt(String jwt) {
        val headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jwt);
        return headers;
    }

    private void setMocks() {
        val metaData =
                MetadataEntity.builder()
                        .id(EXISTING_OBJECT_ID)
                        .fileName("test.bam")
                        .projectCode(EXISTING_PROJECT_CODE)
                        .gnosId(EXISTING_GNOS_ID)
                        .access(CONTROLLED).build();

        when(metadataService.getEntity(EXISTING_GNOS_ID)).thenReturn(metaData);
        when(metadataService.getAnalysisStateForMetadata(metaData)).thenReturn(PUBLISHED);

        val dummyObject = ObjectSpecification.builder().objectId(EXISTING_OBJECT_ID).build();
        when(downloadService.download(eq(EXISTING_OBJECT_ID), anyLong(), anyLong(), anyBoolean(), anyBoolean())).thenReturn(dummyObject);

        val newObjectSpec = ObjectSpecification.builder().objectId(EXISTING_OBJECT_ID).build();
        when(uploadService.initiateUpload(eq(EXISTING_OBJECT_ID), anyByte(), anyString(), anyBoolean())).thenReturn(newObjectSpec);
    }

    enum ScopeOptions {
        VALID_SYSTEM,
        VALID_STUDY,
        INVALID_SYSTEM,
        INVALID_STUDY,
        MALFORMED,
        EMPTY_SCOPE;
    }

    enum RequestType {
        UPLOAD,
        DOWNLOAD
    }
}
