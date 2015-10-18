package org.icgc.dcc.storage.client;

import static com.google.common.base.Objects.firstNonNull;
import static java.lang.System.err;
import static org.icgc.dcc.common.core.util.VersionUtils.getScmInfo;

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
import com.beust.jcommander.Parameter;
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
  private Terminal terminal;
  @Autowired
  private Map<String, ClientCommand> commands;

  public static void main(String[] args) {
    err.print("Starting...");
    try {
      new SpringApplicationBuilder(ClientMain.class).showBanner(false).run(args);
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
        title();
        terminal.println(terminal.label("  Version: ") + getVersion());
        terminal.println(terminal.label("  Built:   ") + getScmInfo().get("git.build.time"));
        terminal.println(terminal.label("  Contact: ") + terminal.email("dcc-support@icgc.org"));
        terminal.println("");
        exit.accept(0);
        return;
      }

      if (help) {
        title();
        usage(cli);
        exit.accept(0);
        return;
      }

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
      usage(cli);

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

  private void title() {
    terminal.printStatus(terminal.label("> ") + terminal.value("ICGC DCC ") + "Storage Client\n");
  }

  private String getCommandName(String beanName) {
    return beanName.replace("Command", "");
  }

  private ClientCommand getCommand(String commandName) {
    val beanName = commandName + "Command";
    return commands.get(beanName);
  }

  private void usage(JCommander cli) {
    val builder = new StringBuilder();
    cli.usage(builder);
    String text = builder.toString();
    text = text.replaceAll("(--\\S+)", "@|bold $1|@");
    text = text.replaceAll("(Options:)", "@|green $1|@");
    text = text.replaceAll("(Commands:)", "@|green $1|@");
    text = text.replaceAll("(download      )", "@|blue $1|@");
    text = text.replaceAll("(manifest      )", "@|blue $1|@");
    text = text.replaceAll("(mount      )", "@|blue $1|@");
    text = text.replaceAll("(upload      )", "@|blue $1|@");
    text = text.replaceAll("(url      )", "@|blue $1|@");
    text = text.replaceAll("(view      )", "@|blue $1|@");
    terminal.println(terminal.ansi(text));
  }

  private String getVersion() {
    return firstNonNull(getClass().getPackage().getImplementationVersion(), "[unknown version]");
  }

}
