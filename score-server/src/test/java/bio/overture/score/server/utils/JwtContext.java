package bio.overture.score.server.utils;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collection;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_EMPTY)
public class JwtContext {

  private JwtScope context;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(NON_EMPTY)
  public static class JwtScope {
    private Collection<String> scope;
  }

  public static JwtContext buildJwtContext(@NonNull Collection<String> scopes) {
    return JwtContext.builder().context(JwtScope.builder().scope(scopes).build()).build();
  }
}
