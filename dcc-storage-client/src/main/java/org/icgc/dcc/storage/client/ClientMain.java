package org.icgc.dcc.storage.client;

import static com.google.common.base.Objects.firstNonNull;
import static java.lang.System.err;
import static org.icgc.dcc.storage.client.cli.Parameters.checkCommand;
import static org.icgc.dcc.storage.client.command.ClientCommand.APPLICATION_NAME;
import static org.icgc.dcc.storage.client.command.ClientCommand.FAILURE_STATUS;
import static org.icgc.dcc.storage.client.util.SingletonBeansInitializer.singletonBeans;

import java.util.List;
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
import com.beust.jcommander.Parameters;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Application entry-point.
 */
@Slf4j
@Configuration
@ComponentScan
@Parameters(separators = "=")
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
  @Parameter(names = "--profile", description = "Define environment profile used to resolve configuration properties", required = false, help = true)
  private String profile = getDefaultProfile();
  @Parameter(names = "--quiet", description = "Reduce output for non-interactive usage", required = false, help = true)
  private boolean quiet = false;
  @Parameter(names = "--silent", description = "Do not produce any informational messages", required = false, help = true)
  private boolean silent = false;
  @Parameter(names = "--version", description = "Show version information", required = false, help = true)
  private boolean version = false;
  @Parameter(names = "--help", description = "Show help information", required = false, help = true)
  private boolean help = false;

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
      // Bootstrap
      val profiles = bootstrap(args);

      // Setup
      val cli = new JCommander();
      cli.setProgramName(APPLICATION_NAME);
      cli.addConverterFactory(new ConverterFactory());

      // Run
      new SpringApplicationBuilder(ClientMain.class)
          .showBanner(false) // Not appropriate for tool
          .initializers(singletonBeans(cli)) // Add cli to context
          .addCommandLineProperties(false) // Only use formal parameters defined in cli
          .profiles(profiles)
          .run(args);

      log.info("Exiting...");
    } catch (Throwable t) {
      // Can't use log here because Spring Boot may not have defined its location yet
      err.println("\nUnknown error starting application: ");
      t.printStackTrace();

      exit(FAILURE_STATUS);
    }
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

      displayHelp();

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

  private void displayHelp() throws Exception {
    val help = getCommand("help");
    help.execute();
  }

  /**
   * This is required to bootstrap arguments for Spring Boot consumption very early in the app lifecycle. The method
   * returns the active profiles.
   */
  private static String[] bootstrap(String[] args) {
    val options = new ClientMain() {

      // Required to ignore the part of args that deals with commands and their options
      @Parameter
      private List<String> values;

    };

    // Digest parse of the command line args
    val cli = new JCommander();
    cli.setAcceptUnknownOptions(true);
    cli.addObject(options);
    cli.parse(args);

    // Pass to spring
    System.setProperty("client.silent", Boolean.toString(options.silent));
    System.setProperty("client.quiet", Boolean.toString(options.quiet));
    System.setProperty("storage.profile", options.profile);

    return options.profile == null ? new String[] {} : new String[] { options.profile };
  }

  private ClientCommand getCommand(String commandName) {
    val beanName = commandName + "Command";
    return commands.get(beanName);
  }

  private static String getCommandName(String beanName) {
    return beanName.replace("Command", "");
  }

  private static String getDefaultProfile() {
    // First try system properties, then environment variables, then default value
    return System.getProperty("storage.profile", firstNonNull(System.getenv("STORAGE_PROFILE"), "aws"));
  }

  private static void exit(int status) {
    exit.accept(status);
  }

}
