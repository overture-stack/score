package bio.overture.score.server.security;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakPermission {
  private String rsid;
  private String rsname;
  private List<String> scopes;
}
