package org.icgc.dcc.storage.test;

import static com.google.common.base.Strings.repeat;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.storage.test.util.Assertions.assertDirectories;
import static org.icgc.dcc.storage.test.util.SpringBootProcess.bootRun;

import java.io.File;
import java.util.List;

import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.metadata.MetadataClient;
import org.icgc.dcc.storage.test.auth.AuthClient;
import org.icgc.dcc.storage.test.fs.FileSystem;
import org.icgc.dcc.storage.test.mongo.Mongo;
import org.icgc.dcc.storage.test.s3.S3;
import org.icgc.dcc.storage.test.util.Port;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.val;

public class StorageIntegrationTest {

  /**
   * Configuration.
   */
  final int authPort = 8443;
  final int metadataPort = 8444;
  final int storagePort = 5431;
  final String gnosId = "70b07570-0571-11e5-a6c0-1697f925ec7b";

  /**
   * State.
   */
  final Mongo mongo = new Mongo();
  final S3 s3 = new S3();
  final FileSystem fs = new FileSystem(new File("target/test"), gnosId);

  @Before
  public void setUp() throws Exception {
    banner("Starting file system...");
    fs.start();

    banner("Starting Mongo...");
    mongo.start();

    banner("Starting S3...");
    s3.start();

    banner("Starting dcc-auth-server...");
    authServer();

    banner("Starting dcc-metadata-server...");
    metadataServer();

    banner("Starting dcc-storage-server...");
    storageServer();

    banner("Waiting for service ports...");
    waitForPort(authPort);
    waitForPort(metadataPort);
    waitForPort(storagePort);
  }

  @After
  public void tearDown() {
    s3.stop();
    mongo.stop();
  }

  @Test
  public void test() throws InterruptedException {

    //
    // Authorize
    //

    banner("Authorizing...");
    val accessToken = new AuthClient("https://localhost:" + authPort).createAccessToken();

    //
    // Register
    //

    banner("Registering...");
    val register = metadataClient(accessToken,
        "-i", fs.getUploadsDir() + "/" + gnosId,
        "-m", "manifest.txt",
        "-o", fs.getRootDir().toString());
    register.waitFor(1, MINUTES);

    assertThat(register.exitValue()).isEqualTo(0);

    //
    // Upload
    //

    banner("Uploading...");
    val upload = storageClient(accessToken,
        "upload",
        "--manifest", fs.getRootDir() + "/manifest.txt");
    upload.waitFor(1, MINUTES);
    assertThat(upload.exitValue()).isEqualTo(0);

    //
    // Find
    //

    val entities = findEntities(gnosId);
    assertThat(entities).isNotEmpty();

    //
    // URL
    //

    banner("URLing " + entities.get(0));
    val url = storageClient(accessToken,
        "url",
        "--object-id", entities.get(0).getId());
    url.waitFor(1, MINUTES);
    assertThat(url.exitValue()).isEqualTo(0);

    //
    // Download
    //

    banner("Downloading...");
    for (val entity : entities) {
      if (isBaiFile(entity)) {
        // Skip BAI files since these will be downloaded when the BAM file is requested
        continue;
      }

      val download = storageClient(accessToken,
          "download",
          "--object-id", entity.getId(),
          "--output-layout", "bundle",
          "--output-dir", fs.getDownloadsDir().toString());
      download.waitFor(1, MINUTES);

      assertThat(download.exitValue()).isEqualTo(0);
    }

    assertDirectories(fs.getDownloadsDir(), fs.getUploadsDir());

    //
    // View
    //

    val bamFile = getBamFile(entities);
    banner("Viewing " + bamFile);
    val view = storageClient(accessToken,
        "view",
        "--header-only",
        "--input-file",
        new File(new File(fs.getDownloadsDir(), bamFile.getGnosId()), bamFile.getFileName()).toString(),
        "--output-type", "sam");
    view.waitFor(1, MINUTES);
    assertThat(view.exitValue()).isEqualTo(0);
  }

  private void authServer() {
    bootRun(
        org.icgc.dcc.auth.server.ServerMain.class,
        "-Dspring.profiles.active=dev,no_scope_validation", // Don't validate if user has scopes
        "-Dlogging.file=" + fs.getLogsDir() + "/dcc-auth-server.log",
        "-Dserver.port=" + authPort,
        "-Dmanagement.port=8543",
        "-Dendpoints.jmx.domain=auth");
  }

  private void metadataServer() {
    bootRun(
        org.icgc.dcc.metadata.server.ServerMain.class,
        "-Dspring.profiles.active=development,secure", // Secure
        "-Dlogging.file=" + fs.getLogsDir() + "/dcc-metadata-server.log",
        "-Dserver.port=" + metadataPort,
        "-Dmanagement.port=8544",
        "-Dendpoints.jmx.domain=metadata",
        "-Dauth.server.url=https://localhost:" + authPort + "/oauth/check_token",
        "-Dauth.server.clientId=metadata",
        "-Dauth.server.clientsecret=pass",
        "-Dspring.data.mongodb.uri=mongodb://localhost:" + mongo.getPort() + "/dcc-metadata");
  }

  private void storageServer() {
    bootRun(
        resolveJarFile("dcc-storage-server"),
        "-Dspring.profiles.active=dev,secure,default", // Secure
        "-Dlogging.file=" + fs.getLogsDir() + "/dcc-storage-server.log",
        "-Dserver.port=" + storagePort,
        "-Dauth.server.url=https://localhost:" + authPort + "/oauth/check_token",
        "-Dauth.server.clientId=storage",
        "-Dauth.server.clientsecret=pass",
        "-Dmetadata.url=https://localhost:" + metadataPort,
        "-Dendpoints.jmx.domain=storage");
  }

  private Process metadataClient(String accessToken, String... args) {
    return bootRun(
        org.icgc.dcc.metadata.client.ClientMain.class,
        args,
        "-Dspring.profiles.active=development",
        "-Dlogging.file=" + fs.getLogsDir() + "/dcc-metadata-client.log",
        "-Dclient.upload.servicePort=" + storagePort,
        "-Dserver.baseUrl=https://localhost:" + metadataPort,
        "-DaccessToken=" + accessToken);
  }

  private Process storageClient(String accessToken, String... args) {
    return bootRun(
        resolveJarFile("dcc-storage-client"),
        args,
        "-Dlogging.file=" + fs.getLogsDir() + "/dcc-storage-client.log",
        "-Dmetadata.url=https://localhost:" + metadataPort,
        "-Dmetadata.ssl.enabled=false",
        "-DaccessToken=" + accessToken);
  }

  private List<Entity> findEntities(String gnosId) {
    val metadataClient = new MetadataClient("https://localhost:" + metadataPort, false);
    return metadataClient.findEntitiesByGnosId(gnosId);
  }

  private static Entity getBamFile(List<Entity> entities) {
    return entities.stream().filter(entity -> entity.getFileName().endsWith(".bam")).findFirst().get();
  }

  private static boolean isBaiFile(Entity entity) {
    return entity.getFileName().endsWith(".bai");
  }

  private static File resolveJarFile(String artifactId) {
    val targetDir = new File("../" + artifactId + "/target");
    return targetDir.listFiles((File file, String name) -> name.startsWith(artifactId) && name.endsWith(".jar"))[0];
  }

  private static void waitForPort(int port) {
    new Port("localhost", port).waitFor(1, MINUTES);
  }

  private static void banner(String text) {
    System.err.println("");
    System.err.println(repeat("#", 100));
    System.err.println(text);
    System.err.println(repeat("#", 100));
    System.err.println("");
  }

}
