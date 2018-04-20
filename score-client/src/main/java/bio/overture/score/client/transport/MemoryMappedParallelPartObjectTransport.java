/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package bio.overture.score.client.transport;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import bio.overture.score.client.download.Downloads;
import bio.overture.score.client.exception.NotResumableException;
import bio.overture.score.client.exception.NotRetryableException;
import bio.overture.score.client.exception.RetryableException;
import bio.overture.score.client.progress.ProgressDataChannel;
import bio.overture.score.core.model.DataChannel;
import bio.overture.score.core.model.Part;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * A data transport using memory mapped channels for parallel upload/download
 * 
 */
@Slf4j
public class MemoryMappedParallelPartObjectTransport extends ParallelPartObjectTransport {

  private static final int FREE_MEMORY_TIME_DELAY = 10;

  @AllArgsConstructor
  private class FreeMemory implements Runnable {

    final private MemoryMappedDataChannel channel;

    @Override
    public void run() {
      try {
        log.debug("Flushing buffer to disk...");
        channel.commitToDisk();
      } finally {
        log.debug("Memory is free: {}", channel.getLength());
        memory.addAndGet(channel.getLength());
      }
    }
  }

  private MemoryMappedParallelPartObjectTransport(RemoteParallelBuilder builder) {
    super(builder);
    log.debug("Transport Settings: {}", builder.toString());
  }

  @Override
  @SneakyThrows
  public void send(File file) {
    log.debug("send file: {}", file.getPath());
    AtomicInteger tasksSubmitted = new AtomicInteger();
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);

    ImmutableList.Builder<Future<Part>> results = ImmutableList.builder();
    progress.start();
    for (final Part part : parts) {
      tasksSubmitted.incrementAndGet();
      try (FileInputStream fis = new FileInputStream(file)) {
        final MappedByteBuffer buffer =
            fis.getChannel().map(FileChannel.MapMode.READ_ONLY, part.getOffset(), part.getPartSize());
        buffer.load();
        // log.debug("Pausing before creating new Callable task");
        // TimeUnit.SECONDS.sleep(3);
        log.debug("Submitting new Callable task");
        // progress.incrementByteRead(part.getPartSize());
        results.add(executor.submit(new Callable<Part>() {

          @Override
          public Part call() throws Exception {
            try {
              DataChannel channel =
                  new ProgressDataChannel(new MemoryMappedDataChannel(buffer, 0, part.getPartSize(), null), progress);
              if (part.isCompleted()) {
                log.info("Checksumming part: {}", part);
                if (checksum && isCorrupted(channel, part, file)) {
                  log.info("Checksum failed; Reuploading part: {}", part);
                  progress.startTransfer();
                  proxy.uploadPart(channel, part, objectId, uploadId);
                }
                progress.incrementChecksumParts();
              } else {
                log.info("Sending remaining part {}", part);
                progress.startTransfer();
                proxy.uploadPart(channel, part, objectId, uploadId);
                progress.incrementParts(1);
              }
            } finally {
              // This is required due to memory mapping which happens natively

              memory.addAndGet(part.getPartSize());
              tasksSubmitted.decrementAndGet();
            }
            return part;
          }
        }));
      }
      long remaining = memory.addAndGet(-part.getPartSize());
      log.debug("Remaining Memory : {}", remaining);
      log.debug("Number of submitted tasks : {}", tasksSubmitted.get());
      while (memory.get() < 0L) {
        log.debug("Memory is low. Wait...");
        TimeUnit.MILLISECONDS.sleep(FREE_MEMORY_TIME_DELAY);
        // suggest to release buffers that are not longer needed (rely on java now)
        // System.gc();
      }
    }

    log.debug("thread pool shut down request ...");
    executor.shutdown();
    executor.awaitTermination(super.maxUploadDuration, TimeUnit.DAYS);
    log.debug("thread pool shut down request completed.");

    progress.stop();
    try {
      takeCareOfException(results.build());
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
    log.debug("Preparing to receive {} in {}", objectId, outputDir.toString());
    File filename = new File(outputDir, objectId);
    long fileSize = Downloads.calculateTotalSize(parts);

    log.debug("Downloading object to file: {}, size:{}", filename.getPath(), fileSize);
    val downloadExecutorService = Executors.newFixedThreadPool(nThreads, new ThreadFactoryBuilder()
        .setNameFormat("downloader-%s").build());
    val memoryCollectorService = Executors.newScheduledThreadPool(Math.max(1, nThreads / 2), new ThreadFactoryBuilder()
        .setNameFormat("memory-cleaner-%s").build());

    val results = new LinkedList<Future<MemoryMappedDataChannel>>();
    progress.start();

    log.debug("Allocating space for file '{}'", filename);
    try (RandomAccessFile fis = new RandomAccessFile(filename, "rw")) {
      fis.setLength(fileSize);
    }
    log.debug("Finished space allocation for file '{}'", filename);

    // This is used to calculate
    if (!Ordering.natural().isOrdered(parts)) {
      Collections.sort(parts);
    }

    boolean hasError = false;
    long prevLength = 0;
    long offset = 0;
    for (final Part part : parts) {
      log.debug("Starting part {} download.", part);
      offset += prevLength;
      prevLength = part.getPartSize();
      val currOffset = offset;

      log.debug("Submitting part # '{}' download.", part.getPartNumber());
      results.push(downloadExecutorService.submit(new Callable<MemoryMappedDataChannel>() {

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
                log.debug("Checking if part #{} is downloaded", part.getPartNumber());
                if (part.isCompleted()) {
                  log.debug("Checking if part #{} is corrupted", part.getPartNumber());
                  if (checksum && isCorrupted(progressChannel, part, outputDir)) {
                    log.debug("Part #{} is corrupted. Re-downloading...", part.getPartNumber());
                    progress.startTransfer();
                    proxy.downloadPart(progressChannel, part, objectId, outputDir);
                    // progress.incrementBytesWritten(part.getPartSize());
                  }
                  progress.incrementChecksumParts();
                } else {
                  log.debug("Part #{} is not downloaded. Downloading...", part.getPartNumber());
                  progress.startTransfer();
                  proxy.downloadPart(progressChannel, part, objectId, outputDir);
                  // progress.incrementBytesWritten(part.getPartSize());
                  progress.incrementParts(1);
                }
                return memoryChannel;
              } catch (RetryableException | NotResumableException | NotRetryableException e) {
                log.error("Failed to receive part: {}", part, e);
                throw e;
              } catch (Throwable e) {
                throw new NotRetryableException(e);
              } finally {
                log.debug("Submitted task for part #{}", part.getPartNumber());
                memoryCollectorService.schedule(new FreeMemory(memoryChannel), FREE_MEMORY_TIME_DELAY, MILLISECONDS);
              }
            }
          }
        } // call()

      })); // results.push(submit(new Callable()))
      memory.addAndGet(-part.getPartSize());
      log.debug("Remaining Memory : {}", memory.get());

      // if we have no free memory, can't process next Part
      while (memory.get() < 0) {
        try {
          if (!results.isEmpty()) {
            Future<MemoryMappedDataChannel> work = results.removeLast();
            // check if the work is done properly instead of just waiting
            work.get();
          }
          TimeUnit.MILLISECONDS.sleep(FREE_MEMORY_TIME_DELAY);
        } catch (ExecutionException e) {
          log.error("Download part failed", e);
          hasError = true;
          if (e.getCause() instanceof NotResumableException) {
            log.error("Download cannot be processed", e);
            // properly shutdown executors
            downloadExecutorService.shutdownNow();
            memoryCollectorService.shutdownNow();
            // then throw immediately
            throw e.getCause();
          }
        }
      }
    } // for (part)

    log.info("all tasks are submitted, waiting for completion...");
    downloadExecutorService.shutdown();
    downloadExecutorService.awaitTermination(super.maxUploadDuration, TimeUnit.DAYS);
    memoryCollectorService.shutdown();
    memoryCollectorService.awaitTermination(super.maxUploadDuration, TimeUnit.DAYS);
    log.info("all tasks are completed");

    progress.stop();
    if (hasError) {
      progress.end(true);
      throw new NotRetryableException(new IOException("some parts failed to download."));
    } else {
      try {
        log.info("finalizing download...");
        takeCareOfException(results);
        proxy.finalizeDownload(outputDir, objectId);
        log.info("Download is finalized");
      } catch (Throwable e) {
        progress.end(true);
        throw e;
      }
    }
    progress.end(false);
  }

  public static MemoryMappedParallelBuilder builder() {
    return new MemoryMappedParallelBuilder();
  }

  public static class MemoryMappedParallelBuilder extends RemoteParallelBuilder {

    @Override
    public Transport build() {
      checkArgumentsNotNull();
      return new MemoryMappedParallelPartObjectTransport(this);
    }
  }
}
