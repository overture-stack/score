package bio.overture.score.server.security;

import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
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
@Getter
public class AuthScope {
  public static final String ALL_PROJECTS = "*";

  private String system;
  private String project;
  private String operation;

  private ScopeSeparators scopeSeparators;

  public AuthScope(String s, ScopeSeparators scopeSeparators) {
    this.scopeSeparators = scopeSeparators;
    val projectSeparator = scopeSeparators.getProjectSeparator();
    val operationSeparator = scopeSeparators.getOperationSeparator();

    if (!s.matches(".*" + operationSeparator + ".*")) {
      throw new IllegalArgumentException(format("Invalid scope '%s' passed to Authscope constructor. " +
        "Operation separator matching regexp %s was not found in that scope", s, operationSeparator));
    }

    if (s.matches(".*" + operationSeparator + ".*" + projectSeparator + ".*")) {
      val temp1 = s.split(projectSeparator, 2);
      this.system = temp1[0].toLowerCase();
      val temp2 = temp1[1].split(operationSeparator, 2);
      this.project = temp2[0].toLowerCase();
      this.operation = temp2[1].toLowerCase();
    } else {
      val temp1 = s.split(operationSeparator, 2);
      this.system = temp1[0].toLowerCase();
      this.operation = temp1[1].toLowerCase();
      this.project = ALL_PROJECTS;
    }
  }

  @Override
  public String toString() {
    return system + scopeSeparators.getProjectSeparator() + project + scopeSeparators.getProjectSeparator() + operation;
  }

  public AuthScope from(@NonNull String scopeStr) {
    return new AuthScope(scopeStr, this.scopeSeparators);
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

  protected List<String> matchingProjects(@NonNull final Collection<AuthScope> scopes) {
    return scopes.stream()
      .filter(this::matches)
      .map(AuthScope::getProject)
      .collect(toUnmodifiableList());
  }

  public boolean allowAllProjects() {
    return ALL_PROJECTS.equals(project);
  }
}
