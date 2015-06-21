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
package collaboratory.storage.object.store.client.transport;

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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import collaboratory.storage.object.store.client.download.DownloadUtils;
import collaboratory.storage.object.store.client.exception.NotResumableException;
import collaboratory.storage.object.store.client.exception.NotRetryableException;
import collaboratory.storage.object.store.client.exception.RetryableException;
import collaboratory.storage.object.store.core.model.Part;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

/**
 * A data transport using memory mapped channels for parallel upload/download
 * 
 */
@Slf4j
public class MemoryMappedParallelPartObjectTransport extends ParallelPartObjectTransport {

  @AllArgsConstructor
  private class FreeMemory implements Runnable {

    final private MemoryMappedDataChannel channel;

    @Override
    public void run() {
      try {
        channel.close();
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
        progress.incrementByteRead(part.getPartSize());
        results.add(executor.submit(new Callable<Part>() {

          @Override
          public Part call() throws Exception {
            try {
              MemoryMappedDataChannel channel = new MemoryMappedDataChannel(buffer, 0, part.getPartSize(), null);
              if (part.isCompleted()) {
                log.debug("Checksumming part: {}", part);
                if (checksum && isCorrupted(channel, part, file)) {
                  log.debug("Fail checksumm. Reupload part: {}", part);
                  proxy.uploadPart(channel, part, objectId,
                      uploadId);
                }
                progress.updateChecksum(1);
              } else {
                proxy.uploadPart(channel, part, objectId,
                    uploadId);
                progress.updateProgress(1);
              }
            } finally {
              progress.incrementByteWritten(part.getPartSize());
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
      while (memory.get() < 0 || tasksSubmitted.get() > queueSize) {
        TimeUnit.MILLISECONDS.sleep(100);
        // suggest to release buffers that are not longer needed
        System.gc();
      }
    }

    log.debug("thread pool shut down request ...");
    executor.shutdown();
    executor.awaitTermination(super.maxUploadDuration, TimeUnit.DAYS);
    progress.stop();
    log.debug("thread pool shut down request completed.");

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
    File filename = new File(outputDir, objectId);
    long fileSize = DownloadUtils.calculateTotalSize(parts);

    log.debug("downloading object to file: {}, size:{}", filename.getPath(), fileSize);
    final ExecutorService downloadExecutorService = Executors.newFixedThreadPool(nThreads);
    final ScheduledExecutorService memoryCollectorService = Executors.newScheduledThreadPool(Math.max(1, nThreads / 2));

    AtomicInteger tasksSubmitted = new AtomicInteger();
    LinkedList<Future<MemoryMappedDataChannel>> results = new LinkedList<Future<MemoryMappedDataChannel>>();
    progress.start();
    try (RandomAccessFile fis = new RandomAccessFile(filename, "rw")) {
      fis.setLength(fileSize);
    }

    // This is used to calculate
    if (!Ordering.natural().isOrdered(parts)) {
      Collections.sort(parts);
    }

    boolean hasError = false;
    long prevLength = 0;
    long offset = 0;
    for (final Part part : parts) {

      offset += prevLength;
      prevLength = part.getPartSize();

      final long currOffset = offset;
      tasksSubmitted.incrementAndGet();
      results.push(downloadExecutorService.submit(new Callable<MemoryMappedDataChannel>() {

        @Override
        public MemoryMappedDataChannel call() throws Exception {
          tasksSubmitted.decrementAndGet();
          try (RandomAccessFile rf = new RandomAccessFile(filename, "rw")) {
            try (FileChannel channel = rf.getChannel()) {
              // TODO: the actual position to position the data block into the file might be different from the original
              // position

              try {
                // 1. Experiment with part number as the position in the file
                final MappedByteBuffer buffer =
                    channel.map(FileChannel.MapMode.READ_WRITE, currOffset, part.getPartSize());
                MemoryMappedDataChannel memoryChannel =
                    new MemoryMappedDataChannel(buffer, part.getOffset(), part.getPartSize(), null);

                if (part.isCompleted()) {
                  if (checksum && isCorrupted(memoryChannel, part, outputDir)) {
                    proxy.downloadPart(memoryChannel, part, objectId, outputDir);
                  }
                  progress.updateChecksum(1);
                } else {
                  proxy.downloadPart(memoryChannel, part, objectId, outputDir);
                  progress.updateProgress(1);
                }
                memoryCollectorService.schedule(new FreeMemory(memoryChannel), 200, TimeUnit.MILLISECONDS);
                return memoryChannel;
              } catch (RetryableException | NotResumableException | NotRetryableException e) {
                log.error("fail to receive part: {}", part, e);
                throw e;
              } catch (Throwable e) {
                throw new NotRetryableException(e);
              }
            }
          } finally {
            progress.incrementByteRead(part.getPartSize());
            progress.incrementByteWritten(part.getPartSize());

          }
        }

      }));
      memory.addAndGet(-part.getPartSize());
      log.debug("Remaining Memory : {}", memory.get());
      log.debug("Number of tasks submitted: {}", tasksSubmitted.get());

      while (memory.get() < 0 || tasksSubmitted.get() > queueSize) {
        try {
          if (!results.isEmpty()) {
            Future<MemoryMappedDataChannel> work = results.removeLast();
            // check if the work is done properly instead of just waiting
            work.get();
          }
          TimeUnit.MILLISECONDS.sleep(500);
        } catch (ExecutionException e) {
          log.error("Download part failed", e);
          hasError = true;
          if (e.getCause() instanceof NotResumableException) {
            throw e.getCause();
          }
        }
      }
    }

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
    public ObjectTransport build() {
      checkArgumentsNotNull();
      return new MemoryMappedParallelPartObjectTransport(this);
    }
  }
}
