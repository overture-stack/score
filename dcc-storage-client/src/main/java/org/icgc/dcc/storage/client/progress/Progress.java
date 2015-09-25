package org.icgc.dcc.storage.client.progress;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.icgc.dcc.storage.client.cli.Terminal;

import com.google.common.base.Stopwatch;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Progress bar for keeping track of the upload/download progress
 */
@Slf4j
public class Progress {

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

  private Terminal terminal;

  public Progress(int total, int numJobs, Terminal terminal) {
    this.total = total;
    this.checksumTotal = total - numJobs;
    this.terminal = terminal;
    this.stopwatch = Stopwatch.createUnstarted();
    updateProgress(total - numJobs);
  }

  public void start() {
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
    terminal.println("");
    terminal.println("Finalizing...");
  }

  public void end(boolean incompleted) {
    if (incompleted) {
      terminal
          .println(terminal.error("Data transfer has been interrupted. Some parts are missing. Waiting to retry..."));
    }

    terminal
        .println(
            terminal.label("Total execution time") + ": " + terminal.value(String.format("%15s", stopwatch.toString())))
        .println(terminal.label("Total bytes read    ") + ": "
            + terminal.value(String.format("%15s", Terminal.formatCount(nByteRead.get()))))
        .println(terminal.label("Total bytes written ") + ": "
            + terminal.value(String.format("%15s", Terminal.formatCount(nByteWritten.get()))));
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
    bar.append(terminal.value(String.format("%3s", percent)));
    bar.append("% [");

    val scale = 0.5f;

    for (int i = 0; i < (int) (100 * scale); i++) {
      val completed = (int) (percent * scale);
      if (i <= completed) {
        bar.append(terminal.label("#"));
      } else if (i == completed + 1) {
        bar.append(" ");
      } else {
        bar.append(".");
      }
    }

    bar.append("] "
        + " "
        + terminal.label("Parts")
        + ": "
        + terminal.value(totalIncr.get() + "/" + total)
        + ", "
        + terminal.label("Checksum")
        + ": "
        + terminal.value(checksumPercent)
        + "%, "
        + terminal.label("Write/sec")
        + ": "
        + terminal.value(Terminal.formatBytes(byteWrittenPerSec)) + Terminal.formatBytesUnits(byteWrittenPerSec) + "/s"
        + ", "
        + terminal.label("Read/sec")
        + ": "
        + terminal.value(Terminal.formatBytes(byteReadPerSec)) + Terminal.formatBytesUnits(byteReadPerSec) + "/s");

    val padding = 4;
    for (int i = 0; i < padding; i++)
      bar.append(" ");

    terminal.print(bar.toString());
  }

}
