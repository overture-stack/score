package org.icgc.dcc.storage.client.cli;

import static com.google.common.base.Strings.repeat;
import static org.fusesource.jansi.Ansi.Color.GREEN;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import jline.TerminalFactory;
import lombok.SneakyThrows;
import lombok.val;

@Component
public class Terminal {

  /**
   * Constants.
   */
  private static final boolean SPIN_WAITING = false;

  /**
   * Configuration.
   */
  private final boolean ansi;
  private final boolean silent;
  private final jline.Terminal delegate;

  @Autowired
  public Terminal(@Value("${client.ansi}") boolean ansi, @Value("${client.silent}") boolean silent) {
    this.ansi = ansi;
    this.silent = silent;

    if (isTty()) {
      // See http://stackoverflow.com/questions/29911077/sbt-hangs-when-running-with-chef-solo
      System.setProperty("jline.terminal", "jline.UnsupportedTerminal");
    }

    this.delegate = TerminalFactory.get();

    Ansi.setEnabled(ansi);
    if (ansi) {
      AnsiConsole.systemInstall();
    }
  }

  public static boolean isTty() {
    // See
    // http://stackoverflow.com/questions/1403772/how-can-i-check-if-a-java-programs-input-output-streams-are-connected-to-a-term
    return System.console() == null;
  }

  public Terminal printLine() {
    println(line());
    return this;
  }

  @SneakyThrows
  @SuppressWarnings("unused")
  public <T> T printWaiting(Callable<T> task) {
    val executor = Executors.newSingleThreadExecutor();
    try {
      val future = executor.submit(task);

      int i = 0;
      while (!future.isDone()) {
        if (ansi && SPIN_WAITING) {
          Thread.sleep(50L);
          val animation = "|/-\\";

          if (i > 0) {
            // Erase
            print(ansi().cursorLeft(2).toString());
          }

          val c = animation.charAt(i++ % animation.length());
          val state = " " + ansi().fg(GREEN).a(c).reset().toString();
          print(state);
        } else {
          Thread.sleep(2000L);
          print(".");
        }
      }

      return future.get();
    } finally {
      executor.shutdownNow();
    }
  }

  @SneakyThrows
  public Terminal printWaiting(Runnable task) {
    printWaiting(() -> {
      task.run();
      return null;
    });

    return this;
  }

  public Terminal printStatus(int stepNumber, String text) {
    return printStatus(step(stepNumber) + " " + text);
  }

  public Terminal printStatus(String text) {
    clearLine();
    return print("\r" + text);
  }

  public Terminal printError(String text, Object... args) {
    clearLine();
    return print(error(text, args) + "\n");
  }

  public Terminal printWarn(String text, Object... args) {
    clearLine();
    return print(warn(text, args) + "\n");
  }

  public Terminal println(String text) {
    print(text + "\n");
    return this;
  }

  public Terminal printf(String text, Object... args) {
    print(String.format(text, args));
    return this;
  }

  public Terminal print(String text) {
    if (!silent) {
      System.err.print(text);
      System.err.flush();
    }
    return this;
  }

  public String ansi(String text) {
    return ansi().render(text).toString();
  }

  public String label(String text) {
    return ansi().render("@|green " + text + "|@").toString();
  }

  public String email(String text) {
    return ansi().render("@|blue,underline " + text + "|@").toString();
  }

  public String url(String text) {
    return ansi().render("@|blue,underline " + text + "|@").toString();
  }

  public String value(long text) {
    return value(Long.toString(text));
  }

  public String error(String text, Object... args) {
    if (args.length == 0) {
      text = text.replace("%", "%%");
    }
    return ansi().render("@|bold,red ERROR: |@").render("@|red " + text + "|@", args).toString();
  }

  public String warn(String text, Object... args) {
    if (args.length == 0) {
      text = text.replace("%", "%%");
    }
    return ansi().render("@|bold,yellow WARN: |@").render("@|yellow " + text + "|@", args).toString();
  }

  public String value(String text) {
    return ansi().bold().render(text).boldOff().toString();
  }

  public String line() {
    return label(Strings.repeat("-", getWidth()));
  }

  public int getWidth() {
    try {
      return delegate.getWidth();
    } catch (Throwable t) {
      return 80;
    }
  }

  public void clearLine() {
    val padding = repeat(" ", getWidth());
    print("\r" + padding + "\r");
  }

  private String step(int stepNumber) {
    return "[" + label(Integer.toString(stepNumber)) + "]";
  }

  private static Ansi ansi() {
    return Ansi.ansi();
  }

  @SuppressWarnings("unused")
  private static String stripAnsi(String text) {
    return jline.internal.Ansi.stripAnsi(text);
  }

}
