package bio.overture.score.server.security;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyIntrospectResponse {
  private long exp;
  private String user_id;
  public List<String> scope;
}
