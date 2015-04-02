package collaboratory.storage.object.store.client;

import java.util.Arrays;
import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import collaboratory.storage.object.store.client.cli.command.ClientCommand;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

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
   * Dependencies.
   */
  @Autowired
  private Map<String, ClientCommand> commands;

  public static void main(String[] args) {
    new SpringApplicationBuilder(ClientMain.class).showBanner(false).run(args);
  }

  @Override
  public void run(String... params) throws Exception {
    // HACK: delete all args with - from the left
    val args = filterSpringConfigurations(params);

    val cli = new JCommander();
    cli.setAcceptUnknownOptions(true);

    cli.setProgramName(APPLICATION_NAME);
    for (val entry : commands.entrySet()) {
      val beanName = entry.getKey();
      val commandName = getCommandName(beanName);
      val command = entry.getValue();

      cli.addCommand(commandName, command);
    }

    try {
      cli.parse(args);
      val commandName = cli.getParsedCommand();
      if (commandName == null) {
        throw new ParameterException("Command name is empty");
      }

      val command = getCommand(commandName);
      if (command == null) {
        throw new ParameterException("Unknown command: " + commandName);
      }

      val status = command.execute();

      System.exit(status);
    } catch (ParameterException e) {
      System.err.println("Missing parameters: " + e.getMessage());
      usage(cli);
      System.exit(1);
    } catch (Exception e) {
      log.error("Unknown error: ", e);
      System.err.println("Command error. Please check the log for detailed error messages: " + e.getMessage());
      usage(cli);
      System.exit(1);
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
    System.err.println(sb.toString());
  }

  private String[] filterSpringConfigurations(String[] args) {
    int lastSpringConfIdx = 0;
    for (val arg : args) {
      if (arg.trim().startsWith("-")) lastSpringConfIdx++;
      else
        break;
    }

    return Arrays.copyOfRange(args, lastSpringConfIdx, args.length);
  }

}
