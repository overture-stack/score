package bio.overture.score.server.repository.azure;

import junit.framework.TestCase;
import lombok.val;
import org.junit.Test;

public class AzureDownloadServiceTest extends TestCase {

  @Test
  public void testContentMD5Conversion() {
    val contentMD5 = "iC9XnDziOQvKwyeOSUfKIg==";
    val converted = AzureDownloadService.base64ToHexMD5(contentMD5);

    assertEquals("882f579c3ce2390bcac3278e4947ca22", converted);
  }
}
