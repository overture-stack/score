package collaboratory.storage.object.store.client.upload;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Stopwatch;

@Slf4j
public class ProgressBar {

  private final AtomicLong totalIncr = new AtomicLong();
  private final AtomicLong nByteWritten = new AtomicLong();
  private final AtomicLong nByteRead = new AtomicLong();

  private final long total;
  private final Stopwatch stopwatch;
  private long byteReadPerSec;
  private long byteWrittenPerSec;
  private long percent;
  private ScheduledExecutorService progressMonitor;
  private final long DELAY = 1L;

  public ProgressBar(long total, int numJobs) {
    this.total = total;
    totalIncr.set(total - numJobs);
    stopwatch = Stopwatch.createUnstarted();
    System.err.println("Number of upload parts remaining: " + numJobs);

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

  public void end() {
    if (stopwatch.isRunning()) {
      stopwatch.stop();
    }
    progressMonitor.shutdownNow();
    try {
      progressMonitor.awaitTermination(DELAY, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      log.debug("cannot stop the stopwatch", e);
    }
    System.err.println();
    System.err.println("Total Time for upload (min): " + (stopwatch.elapsed(TimeUnit.MINUTES) + 1));
  }

  public void incrementByteWritten(long incr) {
    byteWrittenPerSec = nByteWritten.addAndGet(incr) / duration() * 1000;
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

    StringBuilder bar = new StringBuilder("[");

    for (int i = 0; i < 100; i++) {
      if (i < (percent)) {
        bar.append("=");
      } else if (i == (percent)) {
        bar.append(">");
      } else {
        bar.append(" ");
      }
    }

    bar.append("]   " + percent + "%, Write/sec= " + format(byteWrittenPerSec) + ", Read/sec= "
        + format(byteReadPerSec));
    System.err.print("\r" + bar.toString());
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
