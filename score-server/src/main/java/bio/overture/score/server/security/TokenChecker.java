package bio.overture.score.server.security;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.core.Authentication;

@Slf4j
public class TokenChecker {
  public static boolean isExpired(Authentication authentication) {
    log.info("Checking for token expiry...");
    if (authentication instanceof ExpiringOauth2Authentication) {
      val auth = (ExpiringOauth2Authentication) authentication;

      if (auth.getExpiry() == 0) {
        log.info("Expired token detected; authorization denied.");
        return true;
      }
      log.info("Token is not expired; authorization continues.");
      return false;
    }

    log.error("Unknown authentication type detected!");
    return false;
  }
}
