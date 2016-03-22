package org.icgc.dcc.storage.test;

import static com.google.common.base.Objects.firstNonNull;
import static org.icgc.dcc.storage.test.util.SpringBootProcess.bootRun;

import org.icgc.dcc.storage.test.s3.S3Controller;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.val;
import sirius.kernel.di.Injector;
import sirius.web.controller.Controller;

public class StorageIntegrationTest extends AbstractStorageIntegrationTest {

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

  @Test
  @Override
  public void test() throws InterruptedException {
    val block = false;
    registerController(block);

    super.test();
  }

  private void registerController(boolean block) {
    val context = Injector.context();
    val controller = context.wire(new S3Controller(block));
    context.registerDynamicPart(S3Controller.class.getName(), controller, Controller.class);
  }

  @Override
  Process storageServer() {
    int debugPort = Integer.parseInt(System.getProperty("storage.server.debugPort", "-1"));

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
