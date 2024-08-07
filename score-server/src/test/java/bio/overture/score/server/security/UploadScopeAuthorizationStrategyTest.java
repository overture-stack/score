/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package bio.overture.score.server.security;

import static bio.overture.score.server.utils.JwtContext.buildJwtContext;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import bio.overture.score.server.config.SecurityConfig;
import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.metadata.MetadataEntity;
import bio.overture.score.server.metadata.MetadataService;
import bio.overture.score.server.repository.DownloadService;
import bio.overture.score.server.repository.UploadService;
import bio.overture.score.server.security.scope.UploadScopeAuthorizationStrategy;
import bio.overture.score.server.utils.JWTGenerator;
import bio.overture.score.server.utils.JwtContext;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import java.security.KeyPair;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@SpringBootTest
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"test", "secure", "default", "dev"})
public class UploadScopeAuthorizationStrategyTest {
  private static final String TEST_SCOPE = "PROGRAMDATA-TEST1-CA.WRITE";
  private static final String STUDY_PREFIX = "PROGRAMDATA-";
  private static final String UPLOAD_SUFFIX = ".WRITE";
  private static final String SYSTEM_SCOPE = "DCCAdmin.WRITE";

  private static final String PROJECT1 = "TEST1-CA";
  private static final String PROJECT2 = "TEST2-DK";

  private static final String PROVIDER_EGO = "ego";

  // -- Dependencies --
  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private SecurityConfig securityConfig;
  @Autowired private KeyPair keyPair;

  private JWTGenerator jwtGenerator;

  private MockMvc mockMvc;
  @MockBean private MetadataService metadataService;
  @MockBean private DownloadService downloadService;
  @MockBean private UploadService uploadService;

  @Before
  @SneakyThrows
  public void beforeEachTest() {
    jwtGenerator = new JWTGenerator(keyPair);
    if (mockMvc == null) {
      this.mockMvc =
          MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }
  }

  private UploadScopeAuthorizationStrategy sut = init();

  public static UploadScopeAuthorizationStrategy init() {
    val e1 = MetadataEntity.builder().projectCode(PROJECT1).id("1").build();
    val e2 = MetadataEntity.builder().projectCode(PROJECT2).id("2").build();

    val meta = mock(MetadataService.class);
    when(meta.getEntity("1")).thenReturn(e1);
    when(meta.getEntity("2")).thenReturn(e2);

    return new UploadScopeAuthorizationStrategy(
        STUDY_PREFIX, UPLOAD_SUFFIX, SYSTEM_SCOPE, meta, PROVIDER_EGO);
  }

  @SneakyThrows
  private Authentication getAuthentication(Set<String> scopes) {
    ;

    JwtContext jwtContext = buildJwtContext(scopes);
    String jwtString = jwtGenerator.generateJwtWithContext(jwtContext, false);

    long issuedAtMs = Instant.now().toEpochMilli();
    long expiresAtMs = issuedAtMs + HOURS.toMillis(5); // expires in 5 hours from now

    Jwt jwt =
        Jwt.withTokenValue(jwtString)
            .header("typ", "JWT")
            .issuedAt(Instant.ofEpochMilli(issuedAtMs))
            .expiresAt(Instant.ofEpochMilli(expiresAtMs))
            .claims(
                (claims) -> {
                  JSONArray scopeJsonArray = new JSONArray();
                  scopeJsonArray.addAll(scopes);
                  claims.put("context", new JSONObject(Map.of("scope", scopeJsonArray)));
                })
            .build();

    return new JwtAuthenticationToken(jwt);
  }

  @Test
  public void test_system_scope_ok() {
    val scopes = Set.of("test.GBM-US.upload", SYSTEM_SCOPE);
    val authentication = getAuthentication(scopes);
    assertTrue(sut.authorize(authentication, "1"));
  }

  @Test
  public void test_study_scope_wrong_project() {
    val scopes = Set.of(STUDY_PREFIX + PROJECT2 + UPLOAD_SUFFIX, "test.PRAD-US.upload");
    val authentication = getAuthentication(scopes);

    assertFalse(sut.authorize(authentication, "1"));
  }

  @Test
  public void test_study_scope_wrong_access() {
    val scopes = Set.of(STUDY_PREFIX + PROJECT1 + ".READ", STUDY_PREFIX + PROJECT1 + ".upload");
    val authentication = getAuthentication(scopes);
    assertFalse(sut.authorize(authentication, "1"));
  }

  @Test
  public void test_study_scope_ok() {
    val scopes = Set.of(TEST_SCOPE, STUDY_PREFIX + PROJECT1 + ".upload");
    val authentication = getAuthentication(scopes);
    assertTrue(sut.authorize(authentication, "1"));
  }

  @Test
  public void test_study_and_system_scope_ok() {
    val scopes = Set.of(TEST_SCOPE, SYSTEM_SCOPE, STUDY_PREFIX + PROJECT1 + ".upload");
    val authentication = getAuthentication(scopes);
    assertTrue(sut.authorize(authentication, "1"));
  }

  @Test
  public void test_study_scope_unknown_file_fails() {
    val scopes = Set.of("DACO.WRITE", "CLOUD.READ");
    val authentication = getAuthentication(scopes);
    Exception exception = null;
    try {
      sut.authorize(authentication, "NOT-FOUND");
    } catch (NotRetryableException e) {
      exception = e;
    }
    assertNotNull(exception);
    assertEquals(
        "java.lang.IllegalArgumentException: Failed to retrieve metadata for objectId: NOT-FOUND",
        exception.getMessage());
  }

  @Test
  public void test_project_not_looked_up_for_system_scope() {
    val scopes = Set.of(SYSTEM_SCOPE, "DACO.WRITE", "CLOUD.READ");
    val authentication = getAuthentication(scopes);
    Exception exception = null;
    boolean status = false;
    try {
      status = sut.authorize(authentication, "NOT-FOUND");
    } catch (NotRetryableException e) {
      exception = e;
    }
    assertNull(exception);
    assertTrue(status);
  }
}
