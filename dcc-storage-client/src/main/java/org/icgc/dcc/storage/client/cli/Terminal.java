package org.icgc.dcc.storage.client.cli;

import static org.fusesource.jansi.Ansi.ansi;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;

import jline.TerminalFactory;

@Component
public class Terminal {

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

  public String label(String text) {
    return ansi().render("@|green " + text + "|@").toString();
  }

  public String value(long text) {
    return value(Long.toString(text));
  }

  public String error(String text, Object... args) {
    return ansi().render("@|red *** ERROR: " + text + "|@", args).toString();
  }

  public String warn(String text, Object... args) {
    return ansi().render("@|orange *** WARN: " + text + "|@", args).toString();
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

  public static String formatBytes(long bytes) {
    int unit = 1000;
    if (bytes < unit) return Long.toString(bytes);

    int exp = (int) (Math.log(bytes) / Math.log(unit));

    return String.format("%.1f", bytes / Math.pow(unit, exp));
  }

  public static String formatBytesUnits(long bytes) {
    int unit = 1000;
    if (bytes < unit) return "B";

    int exp = (int) (Math.log(bytes) / Math.log(unit));
    return "KMGTPE".charAt(exp - 1) + "";
  }

  public static String formatCount(long count) {
    return String.format("%,d", count);
  }

}
