package collaboratory.storage.object.store;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Application entry point.
 */
@Configuration
@ComponentScan
public class ObjectStoreMain {

  public static void main(String... args) {
    SpringApplication.run(ObjectStoreMain.class, args);
  }

}