package org.icgc.dcc.storage.client.cli;

import static com.google.common.base.Strings.repeat;

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
   * Configuration.
   */
  private final boolean silent;

  @Autowired
  public Terminal(@Value("${client.ansi}") boolean ansi, @Value("${client.silent}") boolean silent) {
    this.silent = silent;

    Ansi.setEnabled(ansi);
    if (ansi) {
      AnsiConsole.systemInstall();
    }
  }

  public Terminal printLine() {
    println(line());
    return this;
  }

  @SneakyThrows
  public <T> T printWaiting(Callable<T> task) {
    val executor = Executors.newSingleThreadExecutor();
    try {
      val future = executor.submit(task);
      while (!future.isDone()) {
        Thread.sleep(2000L);
        print(".");
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
    return print("\n" + error(text, args) + "\n");
  }

  public Terminal printWarn(String text, Object... args) {
    return print("\n" + warn(text, args) + "\n");
  }

  public Terminal print(String text) {
    if (!silent) {
      System.err.print(text);
      System.err.flush();
    }
    return this;
  }

  public Terminal println(String text) {
    print(text + "\n");
    return this;
  }

  public Terminal printf(String text, Object... args) {
    print(String.format(text, args));
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

  private String step(int stepNumber) {
    return "[" + label(Integer.toString(stepNumber)) + "]";
  }

  public String value(long text) {
    return value(Long.toString(text));
  }

  public String error(String text, Object... args) {
    return ansi().render("@|red,bold ERROR:|@ @|red " + text + "|@", args).toString();
  }

  public String warn(String text, Object... args) {
    return ansi().render("@|yellow,bold WARN:|@ @|yellow " + text + "|@", args).toString();
  }

  public String value(String text) {
    return ansi().bold().render(text).boldOff().toString();
  }

  public String line() {
    return label(Strings.repeat("-", getWidth()));
  }

  public int getWidth() {
    return TerminalFactory.get().getWidth();
  }

  public void clearLine() {
    val padding = repeat(" ", getWidth());
    print("\r" + padding);
  }

  private static Ansi ansi() {
    return Ansi.ansi();
  }

  @SuppressWarnings("unused")
  private static String stripAnsi(String text) {
    return jline.internal.Ansi.stripAnsi(text);
  }

}
