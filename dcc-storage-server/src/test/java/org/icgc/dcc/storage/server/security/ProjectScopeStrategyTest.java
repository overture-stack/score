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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import lombok.val;

import org.icgc.dcc.storage.server.security.AuthScope;
import org.icgc.dcc.storage.server.security.ProjectScopeStrategy;
import org.junit.Before;
import org.junit.Test;

public class ProjectScopeStrategyTest {

  public static final String TEST_SCOPE = "test.upload";
  public static final AuthScope testScope = AuthScope.from(TEST_SCOPE);

  private ProjectScopeStrategy _sut;

  @Before
  public void init() {
    _sut = new ProjectScopeStrategy(TEST_SCOPE);
  }

  @Test
  public void test_extract_scopes_handle_multiple() {
    val scopeStrs =
        new HashSet<String>(Arrays.asList("test1.download", "test.download", "test.OTHER-CODE.upload",
            "test.ALT-CODE.upload", "test.CODE.upload", "test2.download"));
    val scopes = _sut.extractScopes(scopeStrs);
    // should only return scopes of format test.{something}.upload
    assertEquals(3, scopes.size());
  }

  @Test
  public void test_extract_scopes_handle_blanket() {
    val scopeStrs = new HashSet<String>(Arrays.asList("test.upload"));
    val scopes = _sut.extractScopes(scopeStrs);
    assertEquals(1, scopes.size());
    assertTrue(scopes.get(0).allowAllProjects());
  }

  @Test
  public void test_extract_scopes_handle_project() {
    val scopeStrs = new HashSet<String>(Arrays.asList("test.PROJ-CODE.upload"));
    val scopes = _sut.extractScopes(scopeStrs);
    assertEquals(1, scopes.size());
    assertFalse(scopes.get(0).allowAllProjects());
    assertEquals("PROJ-CODE", scopes.get(0).getProject());
  }

  @Test
  public void test_scope_filtering() {
    // this represents the list of scopes that are contained in a token
    val scopeStrs = new HashSet<String>(Arrays.asList(TEST_SCOPE, "test.download", "other.upload", "other.upload"));
    val scopes = _sut.extractScopes(scopeStrs);
    val result = _sut.extractProjects(testScope, scopes);
    assertEquals(1, result.size());
  }

  /**
   * Legacy scope format (system.operation) without a project specifier grants blanket authorization to all projects
   */
  @Test
  public void test_scope_internal_wildcard_representation() {
    // this represents the list of scopes that are contained in a token
    val scopeStrs = new HashSet<String>(Arrays.asList(TEST_SCOPE, "test.download", "other.upload", "other.upload"));
    val scopes = _sut.extractScopes(scopeStrs);
    val result = _sut.extractProjects(testScope, scopes);
    assertTrue(result.containsAll(new ArrayList<String>(Arrays.asList(AuthScope.ALL_PROJECTS))));
  }

  @Test
  public void test_check_project_wildcard_short_circuit() throws IOException {
    val scopeStrs =
        new HashSet<String>(Arrays.asList("test.GBM-US.upload", "test.IGNORE-THIS.download", "test.BRCA-US.upload",
            "test.PRAD-US.upload", "test.upload"));
    val scopes = _sut.extractScopes(scopeStrs);
    val projects = _sut.extractProjects(testScope, scopes);
    assertEquals(4, projects.size());

    // returns access without trying to retrieve project code for object id
    assertTrue(_sut.verifyProjectAccess(scopes, "DOESN'T-MATTER"));
  }

  @Test
  public void test_uuid_validation_success() {
    String uuid = "a0bec88a-c5e3-51a9-87bf-3bd6f9c7f23c";
    assertTrue(_sut.validate(uuid));
  }

  @Test
  public void test_uuid_validation_failure() {
    String uuid = "a0bec88a-c5e3-51a9-87bfg-3bd6f9c7f23c";
    assertFalse(_sut.validate(uuid));
  }

}
