package collaboratory.storage.object.store.client.transport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Stopwatch;

/**
 * progress bar for keeping track of the upload/download progress
 */
@Slf4j
public class ProgressBar {

  /**
   * 
   */
  private static final int PADDING = 14;
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
    System.err.println("Number of parts remaining: " + (total - checksumTotal));
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
      System.err.println("Data transfer has been interrupted. Some parts are missing. Waiting to resubmission...");
    }
    System.err.println("Total Execution Time (min): " + (stopwatch.elapsed(TimeUnit.MINUTES) + 1));
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

    for (int i = 0; i < 100; i++) {
      if (i < (percent)) {
        bar.append("=");
      } else if (i == (percent)) {
        bar.append(">");
      } else {
        bar.append(" ");
      }
    }

    bar.append("]   " + percent + "%, Checksum= " + checksumPercent + "%, Write/sec= " + format(byteWrittenPerSec)
        + ", Read/sec= "
        + format(byteReadPerSec));

    for (int i = 0; i < PADDING; i++)
      bar.append(" ");

    System.err.print(bar.toString());
  }

  private String format(long size) {
    if ((size >> 10) == 0) {
      return String.format("%dB/s", size);
    } else if ((size >> 20) == 0) {
      return String.format("%dKB/s", size >> 10);
    } else {
      return String.format("%dMB/s", size >> 20);
    }

  }

}
