/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.storage.client.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.client.download.Downloads;
import org.icgc.dcc.storage.client.exception.NotResumableException;
import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.exception.RetryableException;
import org.icgc.dcc.storage.client.progress.ProgressDataChannel;
import org.icgc.dcc.storage.client.util.JmxUtilities;
import org.icgc.dcc.storage.core.model.DataChannel;
import org.icgc.dcc.storage.core.model.Part;

import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 
 */
@Slf4j
public class TestTransport extends ParallelPartObjectTransport {

  private static final int FREE_MEMORY_TIME_DELAY = 10;

  @AllArgsConstructor
  private class FreeMemory implements Runnable {

    final private MemoryMappedDataChannel channel;

    @Override
    public void run() {
      log.debug("Flushing buffer to disk...");
      channel.commitToDisk();
    }
  }

  private TestTransport(RemoteParallelBuilder builder) {
    super(builder);
    log.debug("Transport Settings: {}", builder.toString());
  }

  protected long calculateThreadCount() {
    // parts list should be sorted prior to this method being called
    long firstPartSize = parts.get(0).getPartSize();
    long lastPartSize = parts.get(parts.size() - 1).getPartSize();
    log.debug("{} parts: part[0] = part #{} (size: {})  part[{}] = part #{} (size: {})", parts.size(),
        parts.get(0).getPartNumber(), firstPartSize,
        parts.size() - 1, parts.get(parts.size() - 1).getPartNumber(), lastPartSize);

    long numThreads = memory.get() / firstPartSize;
    if (numThreads <= 0) {
      numThreads = 1L;
    }
    log.debug("available memory: {} for {} part size = {} threads", memory.get(), firstPartSize, numThreads);
    return (int) numThreads;
  }

  @Override
  @SneakyThrows
  public void send(File file) {
    log.debug("send file: {}", file.getPath());

    // Ensure parts sorted in order
    if (!Ordering.natural().isOrdered(parts)) {
      Collections.sort(parts);
    }

    int numThreads = (int) calculateThreadCount();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);

    val uploadThreadPool =
        Executors.newFixedThreadPool(numThreads, new ThreadFactoryBuilder().setNameFormat("uploader-%s").build());

    val completionService = new ExecutorCompletionService<Part>(uploadThreadPool);

    val futures = new ArrayList<Future<Part>>();
    progress.start();

    for (final Part part : parts) {
      log.debug("Starting part {} upload.", part);
      try (FileInputStream fis = new FileInputStream(file)) {
        final MappedByteBuffer buffer =
            fis.getChannel().map(FileChannel.MapMode.READ_ONLY, part.getOffset(), part.getPartSize());
        buffer.load();
        log.debug("Submitting part # '{}' upload.", part.getPartNumber());

        futures.add(completionService.submit(new Callable<Part>() {

          @Override
          public Part call() throws Exception {
            DataChannel channel =
                new ProgressDataChannel(new MemoryMappedDataChannel(buffer, 0, part.getPartSize(), null), progress);
            if (part.isCompleted()) {
              log.debug("Calculating checksum for part# {}", part);
              if (checksum && isCorrupted(channel, part, file)) {
                log.debug("Checksum failed. Re-upload part# {}", part);
                progress.startTransfer();
                proxy.uploadPart(channel, part, objectId, uploadId);
                progress.incrementBytesWritten(part.getPartSize());
              }
              progress.incrementChecksumParts();
            } else {
              progress.startTransfer();
              proxy.uploadPart(channel, part, objectId, uploadId);
              progress.incrementBytesWritten(part.getPartSize());
              progress.incrementParts(1);
            }

            return part;
          }
        }));
      }
    }

    log.info("All tasks are submitted, waiting for completion...");
    try {
      for (int i = 0; i < parts.size(); ++i) {
        try {
          Part result = completionService.take().get();
        } catch (ExecutionException e) {
          log.error("Upload part failed", e);
          // properly shutdown executors
          uploadThreadPool.shutdownNow();

          if (e.getCause() instanceof NotResumableException) {
            log.error("Upload could not be processed", e);
            // then throw immediately
            throw e.getCause();
          }
        }
      }
    } finally {
      uploadThreadPool.shutdownNow();
      uploadThreadPool.awaitTermination(super.maxTransferDuration, TimeUnit.MINUTES);
      log.debug("Finally block: cancelling remaining {} futures", futures.size());
      for (Future<Part> f : futures) {
        f.cancel(true);
      }
    }

    log.debug("Thread pool shut down request ...");
    executor.shutdown();
    executor.awaitTermination(super.maxTransferDuration, TimeUnit.MINUTES);
    log.debug("Thread pool shut down request completed.");

    progress.stop();
    try {
      proxy.finalizeUpload(objectId, uploadId);
    } catch (Throwable e) {
      progress.end(true);
      throw e;
    }
    progress.end(false);
  }

  @Override
  @SneakyThrows
  public void receive(File outputDir) {
    File filename = new File(outputDir, objectId);
    long fileSize = Downloads.calculateTotalSize(parts);

    // Ensure parts sorted in order
    if (!Ordering.natural().isOrdered(parts)) {
      Collections.sort(parts);
    }

    int numThreads = (int) calculateThreadCount();

    log.debug("Downloading object to file: {}, size:{} using {} threads (and part size {})", filename.getPath(),
        fileSize, numThreads, parts.get(0).getPartSize());

    val downloadThreadPool =
        Executors.newFixedThreadPool(numThreads, new ThreadFactoryBuilder().setNameFormat("downloader-%s").build());

    val completionService = new ExecutorCompletionService<MemoryMappedDataChannel>(downloadThreadPool);

    // val memoryCollectorService =
    // Executors.newScheduledThreadPool(Math.max(1, numThreads / 2),
    // new ThreadFactoryBuilder().setNameFormat("memory-cleaner-%s").build());

    val futures = new ArrayList<Future<MemoryMappedDataChannel>>();
    progress.start();

    log.debug("Allocating space for file '{}'", filename);
    try (RandomAccessFile fis = new RandomAccessFile(filename, "rw")) {
      fis.setLength(fileSize);
    }
    log.debug("Finished space allocation for file '{}'", filename);

    long prevLength = 0;
    long offset = 0;

    // launch threads - hang on to their futures
    for (val part : parts) {
      log.debug("Starting part {} download.", part);
      offset += prevLength;
      prevLength = part.getPartSize();
      val currOffset = offset;

      futures.add(completionService.submit(new Callable<MemoryMappedDataChannel>() {

        @Override
        public MemoryMappedDataChannel call() throws Exception {
          // tasksSubmitted.decrementAndGet();
          try (RandomAccessFile rf = new RandomAccessFile(filename, "rw")) {
            try (FileChannel channel = rf.getChannel()) {
              // TODO: the actual position to position the data block into the file might be different from the original
              // position

              val buffer = channel.map(FileChannel.MapMode.READ_WRITE, currOffset, part.getPartSize());
              log.debug("Created memory buffer of capacity {}", buffer.capacity());
              val memoryChannel = new MemoryMappedDataChannel(buffer, part.getOffset(), part.getPartSize(), null);
              val progressChannel = new ProgressDataChannel(memoryChannel, progress);
              try {
                log.debug("Checking if part #{} has already been downloaded", part.getPartNumber());
                if (part.isCompleted()) {
                  log.debug("Verifying part #{} MD5", part.getPartNumber());
                  if (checksum && isCorrupted(progressChannel, part, outputDir)) {
                    log.debug("MD5 doesn't match for part #{}. Re-downloading...", part.getPartNumber());
                    log.info("*** before GET part# " + part.getPartNumber());
                    logMemoryUsage();
                    progress.startTransfer();
                    proxy.downloadPart(progressChannel, part, objectId, outputDir);
                    progress.incrementBytesWritten(part.getPartSize());
                    if (Thread.interrupted()) {
                      throw new InterruptedException();
                    }
                    log.info("*** After proxy.downloadPart part# " + part.getPartNumber());
                    logMemoryUsage();
                  } else {
                    log.info("MD5 for part #{} validated. Part already downloaded", part.getPartNumber());
                  }
                  progress.incrementChecksumParts();
                } else {
                  log.debug("Part #{} is not downloaded. Downloading...", part.getPartNumber());
                  log.info("*** Before GET part#{} ", part.getPartNumber());
                  logMemoryUsage();
                  progress.startTransfer();
                  proxy.downloadPart(progressChannel, part, objectId, outputDir);
                  progress.incrementBytesWritten(part.getPartSize());
                  progress.incrementParts(1);
                  log.info("*** After proxy.downloadPart part#{} ", part.getPartNumber());
                  logMemoryUsage();
                }
                return memoryChannel;
              } catch (RetryableException | NotResumableException | NotRetryableException e) {
                log.error("Caught exception while downloading part #{}", part, e);
                throw e;
              } catch (Throwable e) {
                throw new NotRetryableException(e);
              } finally {
                // log.debug("finally block: scheduling Free Memory task for part #{}", part.getPartNumber());
                // memoryCollectorService.schedule(new FreeMemory(memoryChannel), FREE_MEMORY_TIME_DELAY, MILLISECONDS);
              }
            }
          }
        } // call()

      })); // results.push(submit(new Callable()))
    } // for (part)

    log.info("All tasks are submitted, waiting for completion...");
    try {
      for (int i = 0; i < parts.size(); ++i) {
        try {
          MemoryMappedDataChannel result = completionService.take().get();
          result.commitToDisk();
        } catch (ExecutionException e) {
          log.error("Download part failed", e);
          // properly shutdown executors
          downloadThreadPool.shutdownNow();
          // memoryCollectorService.shutdownNow();

          // TODO: need to better understand error conditions

          if (e.getCause() instanceof NotResumableException) {
            log.error("Download cannot be processed", e);
            // then throw immediately
            throw e.getCause();
          }
        }
      }
    } finally {
      downloadThreadPool.shutdownNow();
      downloadThreadPool.awaitTermination(super.maxTransferDuration, TimeUnit.MINUTES);
      // memoryCollectorService.shutdown();
      // memoryCollectorService.awaitTermination(super.maxTransferDuration, TimeUnit.MINUTES);
      log.debug("Finally block: cancelling remaining {} futures", futures.size());
      for (Future<MemoryMappedDataChannel> f : futures) {
        f.cancel(true);
      }
      log.info("*** Completion Service finally ");
      logMemoryUsage();
    }

    progress.stop();
    try {
      log.info("Finalizing download...");
      proxy.finalizeDownload(outputDir, objectId);
      log.info("Download is finalized");
    } catch (Throwable e) {
      progress.end(true);
      log.error("Unable to finalize download for object id {}", objectId, e);
      throw e;
    }
    progress.end(false);
    log.info("*** End of receive() ");
    logMemoryUsage();
  }

  public void logMemoryUsage() {
    log.info(JmxUtilities.getHeapUsage());
    log.info(JmxUtilities.getDirectBufferUsage());
    log.info(JmxUtilities.getMappedBufferUsage());
    log.info(JmxUtilities.getNonHeapMemoryProfile());
  }

  public static TestBuilder builder() {
    return new TestBuilder();
  }

  public static class TestBuilder extends RemoteParallelBuilder {

    @Override
    public Transport build() {
      checkArgumentsNotNull();
      return new TestTransport(this);
    }
  }
}
