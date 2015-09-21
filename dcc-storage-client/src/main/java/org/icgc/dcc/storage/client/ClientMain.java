package org.icgc.dcc.storage.client;

import static java.lang.System.err;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import org.icgc.dcc.storage.client.command.ClientCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import com.amazonaws.services.importexport.model.MissingParameterException;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Application entry-point.
 */
@Slf4j
@Configuration
@EnableAutoConfiguration
@EnableRetry
@ComponentScan
public class ClientMain implements CommandLineRunner {

  /**
   * Constants.
   */
  private static final String APPLICATION_NAME = "collab";

  /**
   * Exit handler.
   * <p>
   * This can be changed in testing environments in order to prevent JVM exiting before the test is complete.
   */
  public static Consumer<Integer> exit = System::exit;

  /**
   * Dependencies.
   */
  @Autowired
  private Map<String, ClientCommand> commands;

  public static void main(String[] args) {
    new SpringApplicationBuilder(ClientMain.class).showBanner(false).run(args);
  }

  /**
   * handle user parameters
   */
  @Override
  public void run(String... params) throws Exception {
    // delete all args with - from the left
    String[] args = filterSpringConfigurations(params);

    val cli = new JCommander();
    cli.setAcceptUnknownOptions(true);
    cli.setProgramName(APPLICATION_NAME);

    try {
      for (val entry : commands.entrySet()) {
        val beanName = entry.getKey();
        val commandName = getCommandName(beanName);
        val command = entry.getValue();

        cli.addCommand(commandName, command);

      }

      cli.parse(args);

      val commandName = cli.getParsedCommand();
      if (commandName == null) {
        throw new ParameterException("Command name is empty");
      }

      ClientCommand command = getCommand(commandName);
      if (command == null) {
        throw new ParameterException("Unknown command: " + commandName);
      }

      int status = command.execute();

      exit.accept(status);
    } catch (MissingParameterException e) {
      err.println("Missing parameters: " + e.getMessage());
      usage(cli);

      exit.accept(1);
    } catch (ParameterException e) {
      err.println("Bad parameter(s): " + e.getMessage());
      usage(cli);

      exit.accept(1);
    } catch (Exception e) {
      log.error("Unknown error: ", e);
      err.println("Command error: " + e.getMessage() + "\n\nPlease check the log for detailed error messages");

      exit.accept(1);
    }
  }

  private String getCommandName(final java.lang.String beanName) {
    return beanName.replace("Command", "");
  }

  private ClientCommand getCommand(String commandName) {
    return commands.get(commandName + "Command");
  }

  private void usage(JCommander cli) {
    StringBuilder sb = new StringBuilder();
    cli.usage(sb);
    err.println(sb.toString());
  }

  private String[] filterSpringConfigurations(String[] args) {
    int lastSpringConfIdx = 0;
    for (String arg : args) {
      if (arg.trim().startsWith("-")) lastSpringConfIdx++;
      else
        break;
    }

    return Arrays.copyOfRange(args, lastSpringConfIdx, args.length);
  }

}
