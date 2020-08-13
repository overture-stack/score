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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.KeyPair;
import java.util.List;

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

    private final static String NON_EXISTING_PROJECT_ID = "FAKE";

    private final static String EXISTING_PROJECT_ID = "TEST";
    private final static String EXISTING_OBJECT_ID = "123";
    private final static String EXISTING_GNOS_ID = "123";

    private final static String DOWNLOAD_ENDPOINT = "/download/" + EXISTING_OBJECT_ID + "?offset=0&length=-1";
    private final static String UPLOAD_ENDPOINT = "/upload/" + EXISTING_OBJECT_ID + "/uploads?fileSize=1";

    // -- Dependencies --
    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private SecurityConfig securityConfig;
    @Autowired private KeyPair keyPair;

    // -- Mocking --
    private MockMvc mockMvc;
    @MockBean private MetadataService metadataService;
    @MockBean private DownloadService downloadService;
    @MockBean private UploadService uploadService;

    private JWTGenerator jwtGenerator;

    @Before
    @SneakyThrows
    public void beforeEachTest() {
        jwtGenerator = new JWTGenerator(keyPair);
        if (mockMvc == null) {
            this.mockMvc =
                    MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        }
        setMocks();
    }

    @Test
    public void validateCantDownloadWithNoJwt() {
        val headers = createHeaderWithJwt("");
        val res = executeRequest(HttpMethod.GET, DOWNLOAD_ENDPOINT, headers);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }
    @Test
    public void validateCantDownloadWithExpiredJwt () {
        val res = getResponseFromRequestWithJwt(DOWNLOAD, true, VALID_SYSTEM, true);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }
    @Test
    public void validateCantDownloadWithIncorrectAuthScopes() {
        val res = getResponseFromRequestWithJwt(DOWNLOAD,true, INVALID_STUDY, false);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }
    @Test
    public void validateCanDownloadWithValidAuthScopes() {
        val res = getResponseFromRequestWithJwt(DOWNLOAD, true, VALID_SYSTEM, false);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    public void validateCantUploadWithNoJwt() {
        val headers = createHeaderWithJwt("");
        val res = executeRequest(HttpMethod.POST, UPLOAD_ENDPOINT, headers);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }
    @Test
    public void validateCantUploadWithExpiredJwt() {
        val res = getResponseFromRequestWithJwt(UPLOAD, true, VALID_SYSTEM, true);
        assertEquals(HttpStatus.UNAUTHORIZED, res.getStatusCode());
    }
    @Test
    public void validateCantUploadWithEmptyAuthScopes() {
        val res = getResponseFromRequestWithJwt(UPLOAD, true, EMPTY_SCOPE, false);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }
    @Test
    public void validateCantUploadWithIncorrectAuthScopes() {
        val res = getResponseFromRequestWithJwt(UPLOAD, true, INVALID_STUDY, false);
        assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
    }
    @Test
    public void validateCanUploadWithValidAuthScopes() {
        val res = getResponseFromRequestWithJwt(UPLOAD, true, VALID_SYSTEM, false);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    private  ResponseEntity<String> getResponseFromRequestWithJwt(
            RequestType requestType, boolean hasContext, ScopeOptions scopeOptions, boolean expired) {
        val jwtString = generateConstrainedJWTString(hasContext, scopeOptions, requestType, expired);

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
            boolean hasContext, ScopeOptions scopeOptions, RequestType requestType, boolean expired) {
        JwtContext context = null;
        if (hasContext) {
            if (scopeOptions == VALID_SYSTEM && requestType == DOWNLOAD) {
                context = buildJwtContext(List.of(resolveSystemDownloadScope(), "score.READ", "id.READ"));
            } else if (scopeOptions == VALID_SYSTEM && requestType == UPLOAD) {
                    context = buildJwtContext(List.of(resolveSystemUploadScope(), "score.READ", "id.READ"));
            } else if (scopeOptions == ScopeOptions.VALID_STUDY && requestType == DOWNLOAD) {
                context = buildJwtContext(List.of(resolveStudyDownloadScope(EXISTING_PROJECT_ID), "score.WRITE", "id.READ"));
            } else if (scopeOptions == ScopeOptions.VALID_STUDY && requestType == UPLOAD) {
                context = buildJwtContext(List.of(resolveStudyUploadScope(EXISTING_PROJECT_ID), "score.WRITE", "id.READ"));
            } else if (scopeOptions == INVALID_STUDY && requestType == DOWNLOAD)  {
                context =
                        buildJwtContext(
                                List.of(resolveStudyDownloadScope(NON_EXISTING_PROJECT_ID), "song.WRITE", "song.READ"));
            } else if (scopeOptions == INVALID_STUDY && requestType == UPLOAD) {
                context =
                        buildJwtContext(
                                List.of(resolveStudyUploadScope(NON_EXISTING_PROJECT_ID), "song.WRITE", "song.READ"));
            } else if (scopeOptions == ScopeOptions.INVALID_SYSTEM) {
                context = buildJwtContext(List.of("song.READ", "id.READ"));
            } else if (scopeOptions == ScopeOptions.EMPTY_SCOPE) {
                context = buildJwtContext(List.of());
            } else {
                fail("shouldn't be here");
            }
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
                        .projectCode(EXISTING_PROJECT_ID)
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
        EMPTY_SCOPE;
    }

    enum RequestType {
        UPLOAD,
        DOWNLOAD
    }
}
