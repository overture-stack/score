package org.icgc.dcc.storage.client.progress;

import static org.fusesource.jansi.Ansi.ansi;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.fusesource.jansi.AnsiConsole;

import com.google.common.base.Stopwatch;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Progress bar for keeping track of the upload/download progress
 */
@Slf4j
public class ProgressBar {

  static {
    AnsiConsole.systemInstall();
  }

  private static final int PADDING = 4;
  private final AtomicInteger totalIncr = new AtomicInteger(0);
  private final AtomicInteger checksumTotalIncr = new AtomicInteger(0);
  private final AtomicLong nByteWritten = new AtomicLong(0);
  private final AtomicLong nByteRead = new AtomicLong(0);

  private final int total;
  private final Stopwatch stopwatch;
  private ScheduledExecutorService progressMonitor;
  private final long DELAY = 1L;
  private final int checksumTotal;

  private long byteReadPerSec;
  private long byteWrittenPerSec;
  private long percent;
  private int checksumPercent = 100;

  public ProgressBar(int total, int numJobs) {
    this.total = total;
    this.checksumTotal = total - numJobs;
    updateProgress(total - numJobs);
    stopwatch = Stopwatch.createUnstarted();

  }

  public void start() {
    System.out.println(ansi().render("@|green Number of parts remaining|@: " + (total - checksumTotal)));
    stopwatch.start();
    progressMonitor = Executors.newSingleThreadScheduledExecutor();
    progressMonitor.scheduleWithFixedDelay(new Runnable() {

      @Override
      public void run() {
        display();
      }
    }, DELAY, DELAY, TimeUnit.SECONDS);
  }

  public void stop() {
    if (stopwatch.isRunning()) {
      stopwatch.stop();
    }
    progressMonitor.shutdownNow();
    try {
      progressMonitor.awaitTermination(DELAY, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.debug("cannot stop the stopwatch", e);
    }
    display();
    System.err.println();
    System.err.println("Performing data integrity check, please wait...");
  }

  public void end(boolean incompleted) {
    if (incompleted) {
      System.err.println("Data transfer has been interrupted. Some parts are missing. Waiting to retry...");
    }
    System.err.format("Total execution time: %s%n", stopwatch);
  }

  public void incrementByteWritten(long incr) {
    byteWrittenPerSec = nByteWritten.addAndGet(incr) / duration() * 1000;
  }

  public void updateChecksum(int incr) {
    checksumPercent = checksumTotalIncr.addAndGet(incr) * 100 / checksumTotal;
  }

  private long duration() {
    return stopwatch.elapsed(TimeUnit.MILLISECONDS) + 1;
  }

  public void incrementByteRead(long incr) {
    byteReadPerSec = nByteRead.addAndGet(incr) / duration() * 1000;
  }

  public void updateProgress(int incr) {
    percent = totalIncr.addAndGet(incr) * 100 / total;
  }

  public synchronized void display() {
    StringBuilder bar = new StringBuilder("\r[");

    val scale = 0.5f;

    for (int i = 0; i < (int) (100 * scale); i++) {
      if (i <= (int) (percent * scale)) {
        bar.append(green("◼"));
      } else {
        bar.append(" ");
      }
    }

    bar.append("]   "
        + bold(String.format("%3s", percent))
        + "%,"
        + " "
        + green("Checksum")
        + ": "
        + bold(checksumPercent)
        + "%, "
        + green("Write/sec")
        + ": "
        + bold(format(byteWrittenPerSec))
        + ", "
        + green("Read/sec")
        + ": "
        + bold(format(byteReadPerSec)));

    for (int i = 0; i < PADDING; i++)
      bar.append(" ");

    System.err.print(bar.toString());
    System.err.flush();
  }

  private String format(long size) {
    return formatBytes(size) + "/s";
  }

  public static String formatBytes(long bytes) {
    return formatBytes(bytes, true);
  }

  public static String formatBytes(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;
    if (bytes < unit) return bytes + " B";

    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");

    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  private static String green(String text) {
    return ansi().render("@|green " + text + "|@").toString();
  }

  private static String bold(long text) {
    return bold(Long.toString(text));
  }

  private static String bold(String text) {
    return ansi().bold().render(text).boldOff().toString();
  }

}
