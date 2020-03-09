/*
 * Copyright (c) 2016 - 2019 The Ontario Institute for Cancer Research. All rights reserved.
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

import lombok.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toUnmodifiableList;

/**
 * Utility class modeling a Scope retrieved from dcc-Auth. Traditionally, scopes were two parts joined by a period:
 * prefix.suffix where the prefix was usually a system identifier and the suffix was the permitted operation.
 * <p>
 * With the Storage system, an additional level became necessary to control access to a specific project, for a given
 * operation. These scopes were of the format system.project-code.action, i.e., collab.ABC.upload
 * <p>
 * For backwards compatibility, we have adopted the additional convention of non-project-specific scopes covering all
 * projects. i.e., collab.upload means the scope allows write access to any file in Collaboratory regardless of project
 */
public class AuthScope {

  public static final String ALL_PROJECTS = "*";
  private final String system;
  private final String project;
  private final String operation;
  private final String separator;

  /**
   * @param scopeParts
   */
  public AuthScope(String[] scopeParts, String separator) {
    system = scopeParts[0].toLowerCase();

    if (scopeParts.length == 3) {
      project = scopeParts[1].toUpperCase();
      operation = scopeParts[2].toLowerCase();
    } else {
      operation = scopeParts[1].toLowerCase();
      project = ALL_PROJECTS; // internal representation for "all projects"
    }
    this.separator = separator;
  }

  public boolean matches(AuthScope rule) {
    return (getSystem().equals(rule.getSystem()) && getOperation().equals(rule.getOperation()));
  }

  public List<AuthScope> matchingScopes(@NonNull Set<String> scopeStrings) {
    return scopeStrings.stream()
      .map(this::from)
      .filter(this::matches)
      .collect(toUnmodifiableList());
  }

  private AuthScope from(@NonNull String scopeStr) {
    return from(scopeStr, separator);
  }

  public static AuthScope from(@NonNull String scopeStr, String separator) {
    String[] parts = scopeStr.split(separator);

    if ((parts.length >= 2) && (parts.length <= 3)) {
      return new AuthScope(parts, separator);
    } else {
      throw new IllegalArgumentException(String.format("Invalid scope value received: '%s'", scopeStr));
    }
  }

  protected List<String> matchingProjects(@NonNull final Collection<AuthScope> scopes) {
    return scopes.stream()
      .filter(this::matches)
      .map(AuthScope::getProject)
      .collect(toUnmodifiableList());
  }

  public boolean allowAllProjects() {
    return ALL_PROJECTS.equals(project);
  }

  /**
   * @return the system
   */
  public String getSystem() {
    return system;
  }

  /**
   * @return the project
   */
  public String getProject() {
    return project;
  }

  /**
   * @return the operation
   */
  public String getOperation() {
    return operation;
  }

  @Override
  public String toString() {
    return system + "." + project + "." + operation;
  }
}
