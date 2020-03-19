package bio.overture.score.server.security;

import bio.overture.score.server.metadata.MetadataEntity;
import bio.overture.score.server.metadata.MetadataService;
import lombok.val;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeSet;

import static java.util.Collections.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadAuthorizationTest {
  public static final String TEST_PROJECT = "TEST-CA";
  public static final String GLOBAL_SCOPE = "test.upload";
  public static final String PROJECT_SCOPE="test." + TEST_PROJECT + ".upload";
  public static final String TEST_OBJECT_ID = "123";
  public static final String CONTROLLED_ACCESS = "controlled";
  public static final String OPEN_ACCESS = "open";
  
  @Test
  public void test_authorization_ok_controlled_project_scope() {
    val sut = getSut(CONTROLLED_ACCESS);

    val authentication = getAuthentication(false, "test1.download", "test.OTHER-CODE.upload",
      PROJECT_SCOPE, "test.CODE.upload", "test2.download");
    val result = sut.authorize(authentication, TEST_OBJECT_ID);
    assertTrue(result);
  }

  @Test
  public void test_authorization_ok_controlled_global_scope() {
    val sut = getSut(CONTROLLED_ACCESS);

    val authentication = getAuthentication(false, "test1.download", "test.OTHER-CODE.upload",
      GLOBAL_SCOPE, "test.CODE.upload", "test2.download");
    val result = sut.authorize(authentication, TEST_OBJECT_ID);
    assertTrue(result);
  }


  @Test
  public void test_authorization_fail_open_no_scopes() {
    val sut = getSut(OPEN_ACCESS);

    val authentication = getAuthentication(false);
    val result = sut.authorize(authentication, TEST_OBJECT_ID);
    assertFalse(result);
  }

  @Test
  public void test_authorization_fail_open_no_scopes_expired_token() {
    val sut = getSut(OPEN_ACCESS);

    val authentication = getAuthentication(true);
    val result = sut.authorize(authentication, TEST_OBJECT_ID);
    assertFalse(result);
  }

  @Test
  public void test_authorization_fail_controlled_no_matching_scopes() {
    val sut = getSut(CONTROLLED_ACCESS);

    val authentication = getAuthentication(false, "test1.download", "test.OTHER-CODE.upload",
      "test.ALT-CODE.upload", "test.CODE.upload", "test2.download");
    val result = sut.authorize(authentication, TEST_OBJECT_ID);
    assertFalse(result);
  }

  @Test
  public void test_authorization_fail_controlled_right_project_wrong_operation() {
    val sut = getSut(CONTROLLED_ACCESS);
    val authentication = getAuthentication(false, "test1.download", "test.OTHER-CODE.upload",
      "test." + TEST_PROJECT + ".download", "test.CODE.upload", "test2.download");
    val result = sut.authorize(authentication, TEST_OBJECT_ID);
    assertFalse(result);
  }

  @Test
  public void test_authorization_fail_controlled_right_project_expiredToken() {
    val sut = getSut(CONTROLLED_ACCESS);

    val authentication = getAuthentication(true, "test1.download", "test.OTHER-CODE.upload",
      PROJECT_SCOPE, "test.CODE.upload", "test2.download");
    val result = sut.authorize(authentication, TEST_OBJECT_ID);
    assertFalse(result);
  }

  @Test
  public void test_authorization_fail_controlled_global_expiredToken() {
    val sut = getSut(CONTROLLED_ACCESS);

    val authentication = getAuthentication(true, "test1.download", "test.OTHER-CODE.upload",
      GLOBAL_SCOPE, "test.CODE.upload", "test2.download");
    val result = sut.authorize(authentication, TEST_OBJECT_ID);
    assertFalse(result);
  }

  private Authentication getAuthentication(boolean isExpired, String... scopes) {
    val map = new HashMap<String, Object>();
    map.put("exp", isExpired ? 0 : 3600);
    map.put("scope", new TreeSet<>(Arrays.asList(scopes)));
    return new AccessTokenConverterWithExpiry().extractAuthentication(map);
  }

  public UploadScopeAuthorizationStrategy getSut(String accessType) {
    val metadataService = mock(MetadataService.class);
    val controlledEntity = entity(TEST_OBJECT_ID, accessType);
    when(metadataService.getEntity(TEST_OBJECT_ID)).thenReturn(controlledEntity);

    return new UploadScopeAuthorizationStrategy(GLOBAL_SCOPE, metadataService);
  }

  public MetadataEntity entity(String id, String accessType) {
    val entity = new MetadataEntity();
    entity.setId(id);
    entity.setGnosId("FI12345");
    entity.setFileName("sample1.bam");
    entity.setProjectCode(TEST_PROJECT);
    entity.setAccess(accessType);
    return entity;
  }
}
