package collaboratory.storage.object.store.client.upload;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Stopwatch;

public class ProgressBar {

  private final AtomicLong totalIncr = new AtomicLong();
  private final long total;
  private final Stopwatch stopwatch;

  public ProgressBar(long total, int numJobs) {
    this.total = total;
    totalIncr.set(total - numJobs);
    stopwatch = Stopwatch.createStarted();
    System.err.println("Number of upload parts remaining: " + numJobs);
  }

  public void end() {
    stopwatch.stop();
    System.err.println();
    System.err.println("Total Time for upload (min): " + (stopwatch.elapsed(TimeUnit.MINUTES) + 1));
  }

  public void updateProgress(int incr) {
    long percent = totalIncr.addAndGet(incr) * 100 / total;

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

    bar.append("]   " + percent + "%     ");
    System.err.print("\r" + bar.toString());
  }

}
