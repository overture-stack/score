package org.icgc.dcc.storage.test;

import static com.google.common.base.Objects.firstNonNull;
import static org.icgc.dcc.storage.test.util.SpringBootProcess.bootRun;

import org.junit.After;
import org.junit.Before;

public class StorageIntegrationTest extends AbstractStorageIntegrationTest {

  protected final int storagePort = 5432;

  @Override
  @Before
  public void setUp() throws Exception {
    banner("STORAGE INTEGRATION TEST");
    super.setUp();
  }

  @Override
  @After
  public void tearDown() {
    super.tearDown();
  }

  @Override
  Process storageServer() {
    int debugPort = Integer.parseInt(firstNonNull(System.getProperty("storage.server.debugPort"), "-1"));

    return bootRun(
        resolveJarFile("dcc-storage-server"),
        debugPort,
        "-Dspring.profiles.active=dev,secure,default", // Secure
        "-Dlogging.file=" + fs.getLogsDir() + "/dcc-storage-server.log",
        "-Dserver.port=" + storagePort,
        "-Dbucket.name.object=oicr.icgc.dev",
        "-Dbucket.name.state=oicr.icgc.dev.state",
        "-Dauth.server.url=https://localhost:" + authPort + "/oauth/check_token",
        "-Dauth.server.clientId=storage",
        "-Dauth.server.clientsecret=pass",
        "-Dmetadata.url=https://localhost:" + metadataPort,
        "-Dendpoints.jmx.domain=storage");
  }

  @Override
  Process storageClient(String accessToken, String... args) {
    int debugPort = Integer.parseInt(firstNonNull(System.getProperty("storage.client.debugPort"), "-1"));

    return bootRun(
        resolveJarFile("dcc-storage-client"),
        debugPort,
        args,
        "-Dlogging.file=" + fs.getLogsDir() + "/dcc-storage-client.log",
        "-Dmetadata.url=https://localhost:" + metadataPort,
        "-Dmetadata.ssl.enabled=false",
        "-Dstorage.url=http://localhost:" + storagePort,
        "-DaccessToken=" + accessToken);
  }
}
