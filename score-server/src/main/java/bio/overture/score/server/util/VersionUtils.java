package bio.overture.score.server.util;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Properties;

public class VersionUtils {
  private static final String LOCAL_VERSION = "?";
  private static final Map<String, String> SCM_INFO = loadScmInfo();
  private static final String VERSION =
      (String)
          Objects.firstNonNull(VersionUtils.class.getPackage().getImplementationVersion(), "?");

  public static Map<String, String> getScmInfo() {
    return SCM_INFO;
  }

  public static String getVersion() {
    return VERSION;
  }

  public static String getApiVersion() {
    return "v" + VERSION.split("\\.")[0];
  }

  public static String getCommitId() {
    return (String) Objects.firstNonNull(SCM_INFO.get("git.commit.id.abbrev"), "unknown");
  }

  public static String getCommitMessageShort() {
    return (String) Objects.firstNonNull(SCM_INFO.get("git.commit.message.short"), "unknown");
  }

  private static Map<String, String> loadScmInfo() {
    Properties properties = new Properties();

    try {
      properties.load(VersionUtils.class.getClassLoader().getResourceAsStream("git.properties"));
    } catch (Exception var2) {
    }

    return Maps.fromProperties(properties);
  }

  private VersionUtils() {}
}
