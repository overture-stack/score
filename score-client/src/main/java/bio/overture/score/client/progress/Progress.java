package bio.overture.score.client.progress;

import bio.overture.score.client.cli.Terminal;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static bio.overture.score.client.util.Formats.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Progress bar for keeping track of the upload/download progress.
 */
@Slf4j
public class Progress {

  /**
   * Constants.
   */
  private static final long DISPLAY_INTERVAL = 1L;

  /**
   * Configuration.
   */
  private final boolean quiet;
  private final int totalParts;
  private final int totalChecksumParts;

  /**
   * State - Metrics.
   */
  private final AtomicInteger completedParts = new AtomicInteger(0);
  private volatile long partsPercent;

  private final AtomicInteger completedChecksumParts = new AtomicInteger(0);
  private volatile int checksumPartsPercent = 100;

  private final AtomicLong bytesRead = new AtomicLong(0);
  private volatile long bytesReadPerSec;

  private final AtomicLong bytesWritten = new AtomicLong(0);
  private volatile long bytesWrittenPerSec;

  /**
   * State - Other.
   */
  private final Stopwatch stopwatch = Stopwatch.createUnstarted();
  private volatile ScheduledExecutorService progressMonitor;
  private volatile boolean isTransferStarted;

  /**
   * Dependencies.
   */
  private final Terminal terminal;

  public Progress(Terminal terminal, boolean quiet, int totalParts, int completedParts) {
    this.terminal = terminal;
    this.quiet = quiet;
    this.totalParts = totalParts;
    this.totalChecksumParts = completedParts;

    incrementParts(completedParts);
  }

  public void start() {
    progressMonitor = Executors.newSingleThreadScheduledExecutor();
    progressMonitor.scheduleWithFixedDelay(this::display, DISPLAY_INTERVAL, DISPLAY_INTERVAL, SECONDS);
  }

  public synchronized void startTransfer() {
    if (!isTransferStarted) {
      stopwatch.start();
      isTransferStarted = true;
    }
  }

  public boolean isTransferStarted() {
    return isTransferStarted;
  }

  public void stop() {
    if (stopwatch.isRunning()) {
      stopwatch.stop();
    }

    progressMonitor.shutdownNow();
    try {
      progressMonitor.awaitTermination(DISPLAY_INTERVAL, SECONDS);
    } catch (InterruptedException e) {
      log.debug("Cannot stop the stopwatch: ", e);
    }

    display();
    terminal.println("");
    terminal.println("Finalizing...");
  }

  public void end(boolean incomplete) {
    if (incomplete) {
      terminal
          .println(terminal.error("Data transfer has been interrupted. Some parts are missing. Waiting to retry..."));
    }

    terminal
        .println(
            terminal.label("Total execution time") + ": " + terminal.value(String.format("%15s", stopwatch.toString())))
        .println(terminal.label("Total bytes read    ") + ": "
            + terminal.value(String.format("%15s", formatCount(bytesRead.get()))))
        .println(terminal.label("Total bytes written ") + ": "
            + terminal.value(String.format("%15s", formatCount(bytesWritten.get()))));
  }

  public void incrementParts(int partCount) {
    partsPercent = completedParts.addAndGet(partCount) * 100 / totalParts;
  }

  public void incrementChecksumParts() {
    checksumPartsPercent = completedChecksumParts.addAndGet(1) * 100 / totalChecksumParts;
  }

  public void incrementBytesRead(long byteCount) {
    bytesReadPerSec = bytesRead.addAndGet(byteCount) / duration() * 1000;
  }

  public void incrementBytesWritten(long byteCount) {
    bytesWrittenPerSec = bytesWritten.addAndGet(byteCount) / duration() * 1000;
  }

  private synchronized void display() {
    if (quiet) {
      return;
    }

    val bar = new StringBuilder("\r")
        .append(terminal.value(String.format("%3s", partsPercent)))
        .append("% [");

    val scale = 0.5f;

    for (int i = 0; i < (int) (100 * scale); i++) {
      val completed = (int) (partsPercent * scale);
      if (i <= completed) {
        bar.append(terminal.label("#"));
      } else if (i == completed + 1) {
        bar.append(" ");
      } else {
        bar.append(".");
      }
    }

    bar
        .append("] ")
        .append(" ")
        .append(terminal.label("Parts"))
        .append(": ")
        .append(terminal.value(completedParts.get() + "/" + totalParts))
        .append(", ")
        .append(terminal.label("Checksum"))
        .append(": ")
        .append(terminal.value(checksumPartsPercent))
        .append("%, ")
        .append(terminal.label("Write/sec"))
        .append(": ")
        .append(terminal.value(formatBytes(bytesWrittenPerSec)))
        .append(formatBytesUnits(bytesWrittenPerSec))
        .append("/s")
        .append(", ")
        .append(terminal.label("Read/sec"))
        .append(": ")
        .append(terminal.value(formatBytes(bytesReadPerSec)))
        .append(formatBytesUnits(bytesReadPerSec))
        .append("/s");

    val padding = 4;
    for (int i = 0; i < padding; i++)
      bar.append(" ");

    terminal.print(bar.toString());
  }

  private long duration() {
    return stopwatch.elapsed(MILLISECONDS) + 1;
  }

}
