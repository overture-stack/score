package org.icgc.dcc.storage.client;

import static com.google.common.base.Objects.firstNonNull;
import static java.lang.System.err;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Map;
import java.util.function.Consumer;

import org.icgc.dcc.storage.client.command.ClientCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.MissingCommandException;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.Resources;

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
   * Options.
   */
  @Parameter(names = "--help", description = "shows help message", required = false, help = true)
  private boolean help = false;
  @Parameter(names = "--version", description = "shows version information", required = false, help = true)
  private boolean version = false;

  /**
   * Dependencies.
   */
  @Autowired
  private Map<String, ClientCommand> commands;

  public static void main(String[] args) {
    try {
      new SpringApplicationBuilder(ClientMain.class).showBanner(false).run(args);
    } catch (Throwable t) {
      err.println("Unknown error starting application. Please see log for details");
      log.error("Exception running: ", t);
      exit.accept(1);
    }
  }

  /**
   * Handle user parameters
   */
  @Override
  public void run(String... params) throws Exception {
    val cli = new JCommander(this);
    cli.setAcceptUnknownOptions(true);
    cli.setProgramName(APPLICATION_NAME);
    cli.setColumnSize(TerminalFactory.get().getWidth());

    try {
      for (val entry : commands.entrySet()) {
        val beanName = entry.getKey();
        val commandName = getCommandName(beanName);
        val command = entry.getValue();

        cli.addCommand(commandName, command);
      }

      cli.parse(params);

      if (version) {
        err.println(Resources.toString(Resources.getResource("banner.txt"), UTF_8));
        err.println("Version: " + getVersion());
        exit.accept(0);
        return;
      }

      if (help) {
        usage(cli);
        exit.accept(0);
        return;
      }

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
    } catch (MissingCommandException e) {
      err.println("Missing command: " + e.getMessage());
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

  private String getCommandName(String beanName) {
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

  private String getVersion() {
    return firstNonNull(getClass().getPackage().getImplementationVersion(), "[unknown version]");
  }

}
