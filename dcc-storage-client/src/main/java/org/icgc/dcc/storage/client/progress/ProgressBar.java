package org.icgc.dcc.storage.client.progress;

import static org.fusesource.jansi.Ansi.ansi;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import com.google.common.base.Stopwatch;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Progress bar for keeping track of the upload/download progress
 */
@Slf4j
public class ProgressBar {

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

  private final boolean silent;

  public ProgressBar(int total, int numJobs, boolean ansi, boolean silent) {
    Ansi.setEnabled(ansi);
    if (ansi) {
      AnsiConsole.systemInstall();
    }

    this.total = total;
    this.checksumTotal = total - numJobs;
    this.silent = silent;
    this.stopwatch = Stopwatch.createUnstarted();
    updateProgress(total - numJobs);
  }

  public void start() {
    println(label("Number of parts remaining") + ": " + metric(total - checksumTotal));
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
      log.debug("Cannot stop the stopwatch", e);
    }
    display();
    println("");
    println("Finalizing...");
  }

  public void end(boolean incompleted) {
    if (incompleted) {
      println("Data transfer has been interrupted. Some parts are missing. Waiting to retry...");
    }
    println("Total execution time: " + metric(String.format("%15s", stopwatch.toString())));
    println("Total bytes read:     " + metric(String.format("%15s", formatCount(nByteRead.get()))));
    println("Total bytes written:  " + metric(String.format("%15s", formatCount(nByteWritten.get()))));
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
    StringBuilder bar = new StringBuilder("\r");
    bar.append(metric(String.format("%3s", percent)));
    bar.append("% [");

    val scale = 0.5f;

    for (int i = 0; i < (int) (100 * scale); i++) {
      if (i <= (int) (percent * scale)) {
        bar.append(label("◼"));
      } else {
        bar.append(" ");
      }
    }

    bar.append("] "
        + " "
        + label("Parts")
        + ": "
        + metric(totalIncr.get() + "/" + total)
        + ", "
        + label("Checksum")
        + ": "
        + bold(checksumPercent)
        + "%, "
        + label("Write/sec")
        + ": "
        + metric(format(byteWrittenPerSec))
        + ", "
        + label("Read/sec")
        + ": "
        + metric(format(byteReadPerSec)));

    for (int i = 0; i < PADDING; i++)
      bar.append(" ");

    print(bar.toString());
  }

  private void print(String text) {
    if (!silent) {
      System.err.print(text);
      System.err.flush();
    }
  }

  private void println(String text) {
    print(text + "\n");
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

  public static String formatCount(long count) {
    return String.format("%,d", count);
  }

  private static String label(String text) {
    return ansi().render("@|green " + text + "|@").toString();
  }

  private static String bold(long text) {
    return metric(Long.toString(text));
  }

  private static String metric(long value) {
    return metric(Long.toString(value));
  }

  private static String metric(String text) {
    return ansi().bold().render(text).boldOff().toString();
  }

}
