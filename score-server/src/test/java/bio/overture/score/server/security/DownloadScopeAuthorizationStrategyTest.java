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

import bio.overture.score.server.metadata.MetadataEntity;
import bio.overture.score.server.metadata.MetadataService;
import lombok.val;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadScopeAuthorizationStrategyTest {
  private static final String TEST_SCOPE= "PROGRAMDATA-TEST1-CA.READ";
  private static final String STUDY_PREFIX ="PROGRAMDATA-";
  private static final String DOWNLOAD_SUFFIX =".READ";
  private static final String SYSTEM_SCOPE ="DCCAdmin.WRITE";

  private static final String OPEN_ACESSS_ID = "123"; // file id of a mock open access file
  private static final String CONTROLLED_ACCESS_ID="2345";

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
    return new DownloadScopeAuthorizationStrategy(STUDY_PREFIX, DOWNLOAD_SUFFIX, SYSTEM_SCOPE, getMetadataService());
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

  private Authentication getAuthentication(boolean isExpired, Set<String> scopes) {
    val request = mock(OAuth2Request.class);
    when(request.getScope()).thenReturn(scopes);
    val authentication = mock(ExpiringOauth2Authentication.class);
    when(authentication.getOAuth2Request()).thenReturn(request);
    when(authentication.getExpiry()).thenReturn(isExpired?0:60);
    return authentication;
  }

  @Test
  public void test_open_missing_scope() {
    val auth = getAuthentication(false, Collections.emptySet());
    val result = sut.authorize(auth, OPEN_ACESSS_ID);
    assertTrue(result);
  }

  @Test
  public void test_open_missing_scope_expired() {
    val auth = getAuthentication(true, Collections.emptySet());
    val result = sut.authorize(auth, OPEN_ACESSS_ID);

    assertTrue(result);
  }

  @Test
  public void test_controlled_has_system_scope() {
    val scopes = getAuthentication(false, Set.of("collab.read", "cloud.read", SYSTEM_SCOPE,
      STUDY_PREFIX + "ANOTHER-STUDY-CA"+ DOWNLOAD_SUFFIX));

    val result = sut.authorize(scopes, CONTROLLED_ACCESS_ID);
    assertTrue(result);
  }

  @Test
  public void test_controlled_has_system_scope_expired() {
    val scopes = getAuthentication(true, Set.of("collab.read", "cloud.read", SYSTEM_SCOPE,
      STUDY_PREFIX + "ANOTHER-STUDY-CA"+ DOWNLOAD_SUFFIX));

    val result = sut.authorize(scopes, CONTROLLED_ACCESS_ID);
    assertFalse(result);
  }

  @Test
  public void test_controlled_missing_scope() {
    val scopes = getAuthentication(false, Set.of("collab.read", "cloud.read",
      STUDY_PREFIX + "ANOTHER-STUDY-CA"+ DOWNLOAD_SUFFIX));

    val result = sut.authorize(scopes, CONTROLLED_ACCESS_ID);
    assertFalse(result);
  }

  @Test
  public void test_controlled_has_study_scope() {
    val scopes = getAuthentication(false, Set.of("collab.read", TEST_SCOPE, "other-stuff.download"));
    val result = sut.authorize(scopes, CONTROLLED_ACCESS_ID);
    assertTrue(result);
  }

  @Test
  public void test_controlled_has_study_scope_expired() {
    val scopes = getAuthentication(true, Set.of("collab.read", TEST_SCOPE, "other-stuff.download"));
    val result = sut.authorize(scopes, CONTROLLED_ACCESS_ID);
    assertFalse(result);
  }
}
