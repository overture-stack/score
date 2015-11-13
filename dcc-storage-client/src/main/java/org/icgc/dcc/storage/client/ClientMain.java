package org.icgc.dcc.storage.client;

import static java.lang.System.err;
import static org.icgc.dcc.storage.client.cli.Parameters.checkCommand;
import static org.icgc.dcc.storage.client.command.ClientCommand.APPLICATION_NAME;
import static org.icgc.dcc.storage.client.command.ClientCommand.FAILURE_STATUS;
import static org.icgc.dcc.storage.client.util.SingletonBeansInitializer.singletonBeans;

import java.util.Map;
import java.util.function.Consumer;

import org.icgc.dcc.storage.client.cli.ConverterFactory;
import org.icgc.dcc.storage.client.cli.Terminal;
import org.icgc.dcc.storage.client.command.ClientCommand;
import org.icgc.dcc.storage.client.metadata.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

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
   * Exit handler.
   * <p>
   * This can be changed in testing environments in order to prevent JVM exiting before the test is complete.
   */
  public static Consumer<Integer> exit = System::exit;

  /**
   * Options.
   */
  @Parameter(names = "--quiet", description = "reduce output for non-interactive usage", required = false, help = true)
  private boolean quiet = false;
  @Parameter(names = "--silent", description = "do not produce any informational messages", required = false, help = true)
  private boolean silent = false;
  @Parameter(names = "--help", description = "shows help information", required = false, help = true)
  private boolean help = false;
  @Parameter(names = "--version", description = "shows version information", required = false, help = true)
  private boolean version = false;

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
      // Setup
      bootstrap(args);
      val cli = new JCommander();
      cli.setProgramName(APPLICATION_NAME);
      cli.addConverterFactory(new ConverterFactory());

      // Run
      new SpringApplicationBuilder(ClientMain.class)
          .showBanner(false) // Not appropriate for tool
          .initializers(singletonBeans(cli)) // Add cli to context
          .addCommandLineProperties(false) // Only use formal parameters defined in cli
          .run(args);
    } catch (Throwable t) {
      err.println("\nUnknown error starting application. Please see log for details");
      log.error("Exception running: ", t);

      log.info("Exiting...");
      exit(FAILURE_STATUS);
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
      // Parse
      parseParams(params);

      // Resolve
      val command = resolveCommand();

      // Execute
      exit(command.execute());
    } catch (MissingCommandException e) {
      log.error("Missing command: ", e);
      terminal.printError("Missing command: " + e.getMessage());

      val help = getCommand("help");
      help.execute();

      exit(FAILURE_STATUS);
    } catch (ParameterException e) {
      log.error("Bad parameter(s): ", e);
      terminal.printError("Bad parameter(s): " + e.getMessage());

      exit(FAILURE_STATUS);
    } catch (EntityNotFoundException e) {
      log.error("Entity not found: ", e);
      terminal.printError("Entity not found: " + e.getMessage());

      exit(FAILURE_STATUS);
    } catch (Throwable t) {
      log.error("Unknown error: ", t);
      terminal.printError("Command error: " + t.getMessage() + "\n\nPlease check the log for detailed error messages");

      exit(FAILURE_STATUS);
    }
  }

  private void parseParams(String... params) {
    cli.addObject(this);

    for (val entry : commands.entrySet()) {
      val beanName = entry.getKey();
      val commandName = getCommandName(beanName);
      val command = entry.getValue();

      cli.addCommand(commandName, command);
    }

    cli.parse(params);
  }

  private ClientCommand resolveCommand() {
    if (version) {
      return getCommand("version");
    } else if (help) {
      return getCommand("help");
    } else {
      val commandName = cli.getParsedCommand();
      checkCommand(commandName != null, "Command name is empty. Please specify a command to execute");

      val command = getCommand(commandName);
      checkCommand(command != null, "Unknown command: %s", commandName);

      return command;
    }
  }

  private ClientCommand getCommand(String commandName) {
    val beanName = commandName + "Command";
    return commands.get(beanName);
  }

  private static String getCommandName(String beanName) {
    return beanName.replace("Command", "");
  }

  /**
   * This is required to bootstrap arguments for Spring Boot consumption very early in the app lifecycle.
   */
  private static void bootstrap(String[] args) {
    for (val arg : args) {
      // Set options for Spring Boot property binding
      if (arg.equals("--silent") || arg.equals("--silent=true")) {
        System.setProperty("client.silent", "true");
      }
      if (arg.equals("--quiet") || arg.equals("--quiet=true")) {
        System.setProperty("client.quiet", "true");
      }
    }
  }

  private static void exit(int status) {
    exit.accept(status);
  }

}
