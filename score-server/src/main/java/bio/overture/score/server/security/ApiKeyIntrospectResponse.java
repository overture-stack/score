package bio.overture.score.server.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiKeyIntrospectResponse {
  private long exp;
  private String user_id;
  public List<String> scope;
}
