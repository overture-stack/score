package bio.overture.score.server.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakPermission {
  private String rsid;
  private String rsname;
  private List<String> scopes;
}
