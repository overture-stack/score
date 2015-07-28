package collaboratory.storage.object.store;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;

/**
 * Application entry point.
 */
// @ComponentScan
// @EnableAutoConfiguration(exclude = { org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class,
// org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration.class })
@Slf4j
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class, ManagementSecurityAutoConfiguration.class })
public class ObjectStoreMain {

  public static void main(String... args) {
    SpringApplication.run(ObjectStoreMain.class, args);
  }

}