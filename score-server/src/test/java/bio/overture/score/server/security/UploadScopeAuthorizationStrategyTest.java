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

import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.metadata.MetadataEntity;
import bio.overture.score.server.metadata.MetadataService;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UploadScopeAuthorizationStrategyTest {
  private static final String TEST_SCOPE = "test.upload";
  private static final AuthScope testScope = AuthScope.from(TEST_SCOPE);
  private static final String PROJECT1="TEST1-CA";
  private static final String PROJECT2="TEST2-DK";
  private UploadScopeAuthorizationStrategy sut; // "System Under Test"

  @Before
  public void init() {
    val e1 = MetadataEntity.builder().projectCode(PROJECT1).id("1").build();
    val e2 = MetadataEntity.builder().projectCode(PROJECT2).id("2").build();

    val meta = mock(MetadataService.class);
    when(meta.getEntity("1")).thenReturn(e1);
    when(meta.getEntity("2")).thenReturn(e2);

    sut = new UploadScopeAuthorizationStrategy(TEST_SCOPE, meta);
  }

  @Test
  public void test_extract_scopes_handle_multiple() {
    val scopeStrs =
        new HashSet<String>(Arrays.asList("test1.download", "test.download", "test.OTHER-CODE.upload",
            "test.ALT-CODE.upload", "test.CODE.upload", "test2.download"));
    val scopes = testScope.matchingScopes(scopeStrs);
    // should only return scopes of format test.{something}.upload
    assertEquals(3, scopes.size());
  }

  @Test
  public void test_extract_scopes_handle_blanket() {
    val scopeStrs = new HashSet<String>(Arrays.asList("test.upload"));
    val scopes = testScope.matchingScopes(scopeStrs);
    assertEquals(1, scopes.size());
    assertTrue(scopes.get(0).allowAllProjects());
  }

  @Test
  public void test_extract_scopes_handle_project() {
    val scopeStrs = new HashSet<String>(Arrays.asList("test.PROJ-CODE.upload"));
    val scopes = testScope.matchingScopes(scopeStrs);
    assertEquals(1, scopes.size());
    assertFalse(scopes.get(0).allowAllProjects());
    assertEquals("PROJ-CODE", scopes.get(0).getProject());
  }

  @Test
  public void test_scope_filtering() {
    // this represents the list of scopes that are contained in a token
    val scopeStrs = new HashSet<String>(Arrays.asList(TEST_SCOPE, "test.download", "other.upload", "other.upload"));
    val scopes = testScope.matchingScopes(scopeStrs);
    val result = testScope.matchingProjects(scopes);
    assertEquals(1, result.size());
  }

  /**
   * Legacy scope format (system.operation) without a project specifier grants blanket authorization to all projects
   */
  @Test
  public void test_scope_internal_wildcard_representation() {
    // this represents the list of scopes that are contained in a token
    val scopeStrs = new HashSet<String>(Arrays.asList(TEST_SCOPE, "test.download", "other.upload", "other.upload"));
    val scopes = testScope.matchingScopes(scopeStrs);
    val result = testScope.matchingProjects(scopes);
    assertTrue(result.containsAll(new ArrayList<String>(Arrays.asList(AuthScope.ALL_PROJECTS))));
  }

  @Test
  public void test_check_project_wildcard_short_circuit() throws IOException {
    val scopeStrs =
        new HashSet<String>(Arrays.asList("test.GBM-US.upload", "test.IGNORE-THIS.download", "test.BRCA-US.upload",
            "test.PRAD-US.upload", "test.upload"));
    val scopes = testScope.matchingScopes(scopeStrs);
    val projects = testScope.matchingProjects(scopes);
    assertEquals(4, projects.size());

    // returns access without trying to retrieve project code for object id
    assertTrue(sut.verify(scopes, "DOESN'T-MATTER"));
  }

  @Test
  public void test_check_project_object_wrong_project() {
    val scopeStrs = new HashSet<String>(Arrays.asList("test.GBM-US.upload",
      "test.PRAD-US.upload"));
    val scopes = testScope.matchingScopes(scopeStrs);

    assertFalse(sut.verify(scopes, "1"));

  }

  @Test
  public void test_check_project_object_wrong_access() {
    val scopeStrs = new HashSet<String>(Arrays.asList("test."+PROJECT1+".download",
      "test." + PROJECT2 + ".upload"));
    val scopes = testScope.matchingScopes(scopeStrs);

    assertFalse(sut.verify(scopes, "1"));

  }

  @Test
  public void test_check_project_object_right_access() {
    val scopeStrs = new HashSet<String>(Arrays.asList("test."+PROJECT1+".download",
      "test." + PROJECT2 + ".UPLOAD"));
    val scopes = testScope.matchingScopes(scopeStrs);

    assertTrue(sut.verify(scopes, "2"));

  }

  @Test
  public void test_check_project_object_not_found() {
    val scopeStrs = new HashSet<String>(Arrays.asList("test.GBM-US.upload",
      "test.PRAD-US.upload"));
    val scopes = testScope.matchingScopes(scopeStrs);

    // returns access without trying to retrieve project code for object id
    Exception exception = null;
    try {
      sut.verify(scopes, "NOT-FOUND");
    } catch(NotRetryableException e) {
      exception = e;
    }
    assertNotNull(exception);
    assertEquals("java.lang.IllegalArgumentException: Failed to retrieve metadata for objectId: NOT-FOUND",
      exception.getMessage());
  }

}
