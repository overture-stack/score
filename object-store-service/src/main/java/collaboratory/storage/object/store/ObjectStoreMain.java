package collaboratory.storage.object.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Application entry point.
 */
@ComponentScan
@EnableAutoConfiguration(exclude = { org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class,
    org.springframework.boot.actuate.autoconfigure.ManagementSecurityAutoConfiguration.class })
public class ObjectStoreMain {

  public static void main(String... args) {
    SpringApplication.run(ObjectStoreMain.class, args);
  }

}