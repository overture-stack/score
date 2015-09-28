package org.icgc.dcc.storage.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;

/**
 * Application entry point.
 */
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class, ManagementSecurityAutoConfiguration.class })
public class ServerMain {

  public static void main(String... args) {
    SpringApplication.run(ServerMain.class, args);
  }

}