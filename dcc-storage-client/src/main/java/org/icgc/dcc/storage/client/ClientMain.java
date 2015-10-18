package org.icgc.dcc.storage.client;

import static java.lang.System.err;
import static org.icgc.dcc.storage.client.util.SingletonBeansInitializer.singletonBeans;

import java.util.Map;
import java.util.function.Consumer;

import org.icgc.dcc.storage.client.cli.Terminal;
import org.icgc.dcc.storage.client.command.ClientCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.ParameterException;

import jline.TerminalFactory;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Application entry-point.
 */
@Slf4j
@Configuration
@ComponentScan
public class ClientMain implements CommandLineRunner {

  /**
   * Constants.
   */
  private static final String APPLICATION_NAME = "dcc-storage-client";

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
  private JCommander cli;
  @Autowired
  private Terminal terminal;
  @Autowired
  private Map<String, ClientCommand> commands;

  public static void main(String[] args) {
    err.print("Starting...");
    try {
      val cli = new JCommander(new ClientMain());
      cli.setAcceptUnknownOptions(true);
      cli.setProgramName(APPLICATION_NAME);
      cli.setColumnSize(TerminalFactory.get().getWidth());

      new SpringApplicationBuilder(ClientMain.class).showBanner(false).initializers(singletonBeans(cli)).run(args);
    } catch (Throwable t) {
      err.println("\nUnknown error starting application. Please see log for details");
      log.error("Exception running: ", t);

      log.info("Exiting...");
      exit.accept(1);
    }

    log.info("Exiting...");
  }

  /**
   * Handle user parameters
   */
  @Override
  public void run(String... params) throws Exception {
    terminal.printStatus("Running...");
    try {
      for (val entry : commands.entrySet()) {
        val beanName = entry.getKey();
        val commandName = getCommandName(beanName);
        val command = entry.getValue();

        cli.addCommand(commandName, command);
      }

      cli.parse(params);

      val commandName = cli.getParsedCommand();
      if (commandName == null) {
        throw new ParameterException("Command name is empty. Please specify a command to execute");
      }

      val command = getCommand(commandName);
      if (command == null) {
        throw new ParameterException("Unknown command: " + commandName);
      }

      int status = command.execute();

      exit.accept(status);
    } catch (MissingCommandException e) {
      log.error("Missing command: ", e);
      terminal.printError("Missing command: " + e.getMessage());

      exit.accept(1);
    } catch (ParameterException e) {
      log.error("Bad parameter(s): ", e);
      terminal.printError("Bad parameter(s): " + e.getMessage());

      exit.accept(1);
    } catch (Throwable t) {
      log.error("Unknown error: ", t);
      terminal.printError("Command error: " + t.getMessage() + "\n\nPlease check the log for detailed error messages");

      exit.accept(1);
    }
  }

  private String getCommandName(String beanName) {
    return beanName.replace("Command", "");
  }

  private ClientCommand getCommand(String commandName) {
    val beanName = commandName + "Command";
    return commands.get(beanName);
  }

}
