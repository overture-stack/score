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
package org.icgc.dcc.storage.server.security;

import lombok.val;
import org.icgc.dcc.storage.server.jwt.JWTUser;
import org.icgc.dcc.storage.server.metadata.MetadataEntity;
import org.icgc.dcc.storage.server.metadata.MetadataService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@ActiveProfiles("jwt")
public class BasicJWTAuthorizationStrategyTest {

    @Mock
    private MetadataService metadataService;

    private BasicJWTAuthorizationStrategy sut;

    private JWTUser unapprovedUser;
    private JWTUser approvedWithAccessUser;
    private JWTUser approvedWithoutAccessUser;

    public static final String TEST_SCOPE = "test.download";
    public static final AuthScope testScope = AuthScope.from(TEST_SCOPE);

    @Before
    public void init() {
        sut = new BasicJWTAuthorizationStrategy(TEST_SCOPE);
        sut.setMetadataService(metadataService);

        // Mock JWTUser's
        val userBuilder = JWTUser
                .builder()
                .name("Demo.User@example.com")
                .firstName("Demo")
                .lastName("User")
                .email("Demo.User@example.com")
                .createdAt("2017-11-22 03:10:55")
                .lastLogin("2017-12-08 07:43:02")
                .preferredLanguage(null)
                .status("Approved");

        unapprovedUser = userBuilder.build();
        approvedWithAccessUser = userBuilder.build();
        approvedWithoutAccessUser = userBuilder.build();

        // Careful with underlying references
        unapprovedUser.setStatus("Not Approved");
        approvedWithAccessUser.setRoles(Arrays.asList("test.download"));
        approvedWithoutAccessUser.setRoles(Arrays.asList("test2.download"));
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

    @Test
    public void test_open_no_scope() {
        val openEntity = entity("123", "abc", "something.bam", "PROJ-CD", "open");
        when(metadataService.getEntity("123")).thenReturn(openEntity);

        // no test.download scope
        val scopeStrs =
                new HashSet<String>(Arrays.asList("test1.download", "test.OTHER-CODE.upload",
                        "test.ALT-CODE.upload", "test.CODE.upload", "test2.download"));
        val scopes = sut.extractScopes(scopeStrs);

        val result = sut.verify(scopes, "123");
        assertTrue(result);
    }

    @Test
    public void test_open_no_token() {
        val openEntity = entity("123", "abc", "something.bam", "PROJ-CD", "open");
        when(metadataService.getEntity("123")).thenReturn(openEntity);

        // no token, means no scopes at all
        List<AuthScope> scopes = Collections.<AuthScope>emptyList();

        val result = sut.verify(scopes, "123");
        assertTrue(result);
    }

    @Test
    public void test_controlled() {
        val openEntity = entity("123", "abc", "something.bam", "PROJ-CD", "controlled");
        when(metadataService.getEntity("123")).thenReturn(openEntity);

        val scopeStrs =
                new HashSet<String>(Arrays.asList("test1.download", "test.download", "test.OTHER-CODE.upload",
                        "test.ALT-CODE.upload", "test.CODE.upload", "test2.download"));
        List<AuthScope> scopes = sut.extractScopes(scopeStrs);

        val result = sut.verify(scopes, "123");
        assertTrue(result);
    }

    @Test
    public void test_controlled_no_scope() {
        val openEntity = entity("123", "abc", "something.bam", "PROJ-CD", "controlled");
        when(metadataService.getEntity("123")).thenReturn(openEntity);

        // missing test.download scope
        val scopeStrs =
                new HashSet<String>(Arrays.asList("test1.download", "test.OTHER-CODE.upload",
                        "test.ALT-CODE.upload", "test.CODE.upload", "test2.download"));
        List<AuthScope> scopes = sut.extractScopes(scopeStrs);

        val result = sut.verify(scopes, "123");
        assertFalse(result);
    }

    @Test
    public void test_verify_basic_scopes_happy_path() {
        val scopeStrs =
                new HashSet<String>(Arrays.asList("test1.download", "test.download", "test.OTHER-CODE.upload",
                        "test.ALT-CODE.upload", "test.CODE.upload", "test2.download"));
        List<AuthScope> scopes = sut.extractScopes(scopeStrs);

        assertTrue(sut.verifyBasicScope(scopes));
    }

    @Test
    public void test_verify_basic_scopes_no_match() {
        val scopeStrs =
                new HashSet<String>(Arrays.asList("test1.download", "test.OTHER-CODE.upload",
                        "test.ALT-CODE.upload", "test.CODE.upload", "test2.download"));
        List<AuthScope> scopes = sut.extractScopes(scopeStrs);

        assertFalse(sut.verifyBasicScope(scopes));
    }

    @Test
    public void testJWTUserWithAccess() throws Exception {
        val openEntity = entity("123", "abc", "something.bam", "PROJ-CD", "controlled");
        when(metadataService.getEntity("123")).thenReturn(openEntity);

        val roles = new HashSet<String>(approvedWithAccessUser.getRoles());
        val scopes = sut.extractScopes(roles);
        val isVerified = sut.verify(scopes, "123");
        assertThat(isVerified).isTrue();
    }

    @Test
    public void testJWTUserWithoutAccess() throws Exception {
        val openEntity = entity("123", "abc", "something.bam", "PROJ-CD", "controlled");
        when(metadataService.getEntity("123")).thenReturn(openEntity);

        val roles = new HashSet<String>(approvedWithoutAccessUser.getRoles());
        val scopes = sut.extractScopes(roles);
        val isVerified = sut.verify(scopes, "123");
        assertThat(isVerified).isFalse();
    }
}
