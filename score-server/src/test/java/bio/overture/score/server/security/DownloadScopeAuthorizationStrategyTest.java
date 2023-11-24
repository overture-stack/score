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

import bio.overture.score.server.config.SecurityConfig;
import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.metadata.MetadataEntity;
import bio.overture.score.server.metadata.MetadataService;
import bio.overture.score.server.repository.DownloadService;
import bio.overture.score.server.repository.UploadService;
import bio.overture.score.server.security.scope.DownloadScopeAuthorizationStrategy;
import bio.overture.score.server.utils.JWTGenerator;
import bio.overture.score.server.utils.JwtContext;
import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import lombok.SneakyThrows;
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

import java.security.KeyPair;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static bio.overture.score.server.utils.JwtContext.buildJwtContext;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"test", "secure", "default", "dev"})
public class DownloadScopeAuthorizationStrategyTest {

  private static final String STUDY_PREFIX = "PROGRAMDATA-";
  private static final String DOWNLOAD_SUFFIX = ".READ";
  private static final String SYSTEM_SCOPE = "DCCAdmin.WRITE";
  private static final String TEST_STUDY = "TEST1-CA";
  private static final String OTHER_STUDY = "TEST2-DK";

  private static final String OPEN_ACESSS_ID = "123"; // file id of a mock open access file
  private static final String CONTROLLED_ACCESS_ID = "2345";

  private static final String PROVIDER_EGO = "ego";

  // -- Dependencies --
  @Autowired
  private WebApplicationContext webApplicationContext;
  @Autowired private SecurityConfig securityConfig;
  @Autowired private KeyPair keyPair;

  private JWTGenerator jwtGenerator;
  private MockMvc mockMvc;
  @MockBean
  private MetadataService metadataService;
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

  private DownloadScopeAuthorizationStrategy sut = init(); // System Under Test

  public MetadataService getMetadataService() {
    val metadataService = mock(MetadataService.class);

    val openEntity = entity(OPEN_ACESSS_ID, "abc1", "something.bam", "TEST1-CA",
      "open");
    when(metadataService.getEntity(OPEN_ACESSS_ID)).thenReturn(openEntity);

    val controlledEntity = entity(CONTROLLED_ACCESS_ID, "abc2", "something-else.bam",
      "TEST1-CA", "controlled");
    when(metadataService.getEntity(CONTROLLED_ACCESS_ID)).thenReturn(controlledEntity);

    return metadataService;
  }

  public DownloadScopeAuthorizationStrategy init() {
    return new DownloadScopeAuthorizationStrategy(STUDY_PREFIX, DOWNLOAD_SUFFIX, SYSTEM_SCOPE, getMetadataService(), PROVIDER_EGO);
  }

  public MetadataEntity entity(String id, String gnosId, String fileName, String projectCode, String accessType) {
    val entity = new MetadataEntity();
    entity.setId(id);
    entity.setGnosId(gnosId);
    entity.setFileName(fileName);
    entity.setProjectCode(projectCode);
    entity.setAccess(accessType);
    return entity;
  }

  private Authentication getAuthentication(Set<String> scopes) {
    JwtContext jwtContext = buildJwtContext(scopes);
    String jwtString = jwtGenerator.generateJwtWithContext(jwtContext, false);

    long issuedAtMs = Instant.now().toEpochMilli();
    long expiresAtMs = issuedAtMs + HOURS.toMillis(5); // expires in 5 hours from now

    Jwt jwt = Jwt
        .withTokenValue(jwtString)
        .header("typ","JWT")
        .issuedAt(Instant.ofEpochMilli(issuedAtMs))
        .expiresAt(Instant.ofEpochMilli(expiresAtMs))
        .claims((claims) -> {
          JSONArray scopeJsonArray = new JSONArray();
          scopeJsonArray.addAll(scopes);
          claims.put("context", new JSONObject(Map.of("scope", scopeJsonArray)));
        })
        .build();

    return new JwtAuthenticationToken(jwt);
  }

  public String getStudyScope(String study) {
    return STUDY_PREFIX + study + DOWNLOAD_SUFFIX;
  }

  public Set<String> getScopes(boolean hasSystem, boolean hasStudy, boolean hasOther) {
    val scopes = new TreeSet<String>();
    if (hasSystem) {
      scopes.add(SYSTEM_SCOPE);
    }
    if (hasStudy) {
      scopes.add(getStudyScope(TEST_STUDY));
    }
    if (hasOther) {
      scopes.add(getStudyScope(OTHER_STUDY));
    }
    return scopes;
  }

  public boolean run_test(boolean isOpen, boolean hasSystem, boolean hasStudy, boolean hasOther) {
    val scopes = getScopes(hasSystem, hasStudy, hasOther);
    val auth = getAuthentication(scopes);
    return sut.authorize(auth, isOpen ? OPEN_ACESSS_ID : CONTROLLED_ACCESS_ID);
  }

  @Test
  public void test_non_expired_open_access_always_succeeds() {
    val choices = List.of(false, true);
    boolean everythingPassed = true;
    for (val hasSystem : choices) {
      for (val hasStudy : choices) {
        for (val hasOther : choices) {
          val result = run_test(true, hasSystem, hasStudy, hasOther);
          if (!result) {
            System.err.printf("Open access wasn't granted to non-expired token (scopes='%s')",
              getScopes(hasSystem, hasStudy, hasOther));
            everythingPassed = false;
          }
        }
      }
    }
    assertTrue(everythingPassed);
  }

  @Test
  public void test_controlled_access_no_scopes_fails() {
    assertFalse(run_test(false, false, false, false));
  }

  @Test
  public void test_controlled_access_wrong_scope_fails() {
    assertFalse(run_test(false, false, false, true));
  }

  @Test
  public void test_controlled_access_non_expired_study_scope_succeeds() {
    val choices = List.of(false, true);
    boolean everythingPassed = true;
    for (val hasOther : choices) {
      val result = run_test(false, false, true, hasOther);
      if (!result) {
        System.err.printf("Access wasn't granted to non-expired token (scopes='%s')",
          getScopes(false, true, hasOther));
        everythingPassed = false;
      }
    }
    assertTrue(everythingPassed);
  }

  @Test
  public void test_controlled_access_non_expired_system_scope_succeeds() {
    val choices = List.of(false, true);
    boolean everythingPassed = true;
    for (val hasStudy : choices) {
      for (val hasOther : choices) {
        val result = run_test(false, true, hasStudy, hasOther);
        if (!result) {
          System.err.printf("Controlled access wasn't granted to non-expired token (scopes='%s')",
            getScopes(true, hasStudy, hasOther));
          everythingPassed = false;
        }
      }
    }
    assertTrue(everythingPassed);
  }

  @Test
  public void test_controlled_study_scope_wrong_access_fails() {
    val scopes = Set.of(STUDY_PREFIX + TEST_STUDY + ".wrong");
    val auth = getAuthentication(scopes);
    assertFalse(sut.authorize(auth, CONTROLLED_ACCESS_ID));
  }

  @Test
  public void test_controlled_system_scope_wrong_access_fails() {
    val scopes = Set.of("DCCAdmin.wrong");
    val auth = getAuthentication(scopes);
    assertFalse(sut.authorize(auth, CONTROLLED_ACCESS_ID));
  }

  @Test
  public void test_project_study_object_does_not_exist_fails() {
    val scopes = Set.of(STUDY_PREFIX + TEST_STUDY + DOWNLOAD_SUFFIX);
    val auth = getAuthentication(scopes);
    Exception exception = null;
    try {
      assertFalse(sut.authorize(auth, "non-existent"));
    } catch (NotRetryableException e) {
      exception = e;
    }
    assertNotNull(exception);
    assertEquals("java.lang.IllegalArgumentException: Failed to retrieve metadata for objectId: non-existent",
      exception.getMessage());
  }

  @Test
  public void test_system_scope_object_not_looked_up() {
    val scopes = Set.of(SYSTEM_SCOPE);
    val auth = getAuthentication(scopes);
    Exception exception = null;
    boolean status = false;
    try {
      status = sut.authorize(auth, "non-existent");
    } catch (NotRetryableException e) {
      exception = e;
    }
    assertNull(exception);
    assertTrue(status);
  }
}
