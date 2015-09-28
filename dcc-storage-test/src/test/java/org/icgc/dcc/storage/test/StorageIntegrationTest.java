package org.icgc.dcc.storage.test;

import static org.icgc.dcc.storage.test.util.BootProcess.bootProcess;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.icgc.dcc.storage.test.auth.AuthClient;
import org.icgc.dcc.storage.test.mongo.Mongo;
import org.icgc.dcc.storage.test.s3.S3;
import org.icgc.dcc.storage.test.util.Port;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StorageIntegrationTest {

  Mongo mongo;

  @Before
  public void setUp() throws Exception {
    log.info("Starting mongo...");
    mongo = new Mongo();
    mongo.start();

    log.info("Starting S3...");
    new S3().start();

    log.info("Starting auth server...");
    bootProcess(
        org.icgc.dcc.auth.server.ServerMain.class,
        "--spring.profiles.active=dev,no_scope_validation", // Don't validate if user has scopes
        "--server.port=8443",
        "--management.port=8543",
        "--endpoints.jmx.domain=auth");

    log.info("Starting metadata server...");
    bootProcess(
        org.icgc.dcc.metadata.server.ServerMain.class,
        "--spring.profiles.active=development",
        "--server.port=8444",
        "--management.port=8544",
        "--endpoints.jmx.domain=metadata",
        "--auth.server.url=https://localhost:8443/oauth/check_token",
        "--auth.server.clientId=metadata",
        "--auth.server.clientsecret=pass",
        "--spring.data.mongodb.uri=mongodb://localhost:" + mongo.getPort() + "/dcc-metadata");

    log.info("Starting storage server...");
    bootProcess(
        new File("../dcc-storage-server/target/dcc-storage-server-0.0.22-SNAPSHOT.jar"),
        "--spring.profiles.active=dev,default",
        "--auth.server.url=https://localhost:8443/oauth/check_token",
        "--auth.server.clientId=storage",
        "--auth.server.clientsecret=pass",
        "--metadata.url=https://localhost:8444/entities",
        "--endpoints.jmx.domain=storage");

    new Port("localhost", 8443).waitFor(30, TimeUnit.SECONDS);
    new Port("localhost", 8444).waitFor(30, TimeUnit.SECONDS);
    new Port("localhost", 5431).waitFor(30, TimeUnit.SECONDS);
  }

  @After
  public void tearDown() {
    mongo.stop();
  }

  @Test
  public void test() throws InterruptedException {
    val accessToken = new AuthClient("https://localhost:8443").createAccessToken();
    log.info("accessToken: {}", accessToken);

    val process = bootProcess(
        org.icgc.dcc.metadata.client.ClientMain.class,
        "--spring.profiles.active=development",
        "--server.baseUrl=https://localhost:8444",
        "--accessToken=" + accessToken,
        "-i", "src/test/resources/fixtures/70b07570-0571-11e5-a6c0-1697f925ec7b",
        "-m", "manifest.txt",
        "-o", "target");

    process.waitFor();
  }

}
