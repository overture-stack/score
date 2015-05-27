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
package collaboratory.storage.object.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import collaboratory.storage.object.store.core.model.Part;

import com.google.common.collect.ImmutableList;

/**
 * A data transport using memory mapped channels for parallel upload/download
 * 
 */
@Slf4j
public class MemoryMappedParallelPartObjectTransport extends ParallelPartObjectTransport {

  private MemoryMappedParallelPartObjectTransport(RemoteParallelBuilder builder) {
    super(builder);
  }

  @Override
  @SneakyThrows
  public void send(File file) {
    log.debug("send file: {}", file.getPath());
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    ImmutableList.Builder<Future<Part>> results = ImmutableList.builder();
    progress.start();
    for (final Part part : parts) {
      try (FileInputStream fis = new FileInputStream(file)) {
        final MappedByteBuffer buffer =
            fis.getChannel().map(FileChannel.MapMode.READ_ONLY, part.getOffset(), part.getPartSize());
        buffer.load();
        results.add(executor.submit(new Callable<Part>() {

          @Override
          public Part call() throws Exception {
            MemoryMappedDataChannel channel = new MemoryMappedDataChannel(buffer, 0, part.getPartSize(), null);
            if (part.isCompleted()) {
              if (isCorrupted(channel, part, file)) {
                proxy.uploadPart(channel, part, objectId,
                    uploadId);
              }
              progress.updateChecksum(1);
            } else {
              proxy.uploadPart(channel, part, objectId,
                  uploadId);
              progress.updateProgress(1);
            }
            progress.incrementByteWritten(part.getPartSize());
            memory.addAndGet(part.getPartSize());
            return part;
          }
        }));
      }
      progress.incrementByteRead(part.getPartSize());
      long remaining = memory.addAndGet(-part.getPartSize());
      log.debug("Remaining Memory : {}", remaining);
      while (memory.get() < 0) {
        TimeUnit.MILLISECONDS.sleep(100);
        // suggest to release buffers that are not longer needed
        System.gc();
      }
    }
    executor.shutdown();
    executor.awaitTermination(super.maxUploadDuration, TimeUnit.DAYS);
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
    log.debug("receive file: {}", filename.getPath());
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    LinkedList<Future<MemoryMappedDataChannel>> results = new LinkedList<Future<MemoryMappedDataChannel>>();
    progress.start();
    try (RandomAccessFile fis = new RandomAccessFile(filename, "rw")) {
      fis.setLength(calculateTotalSize(parts));
    }

    boolean hasError = false;
    boolean shouldThrottled = false;
    for (final Part part : parts) {
      results.add(executor.submit(new Callable<MemoryMappedDataChannel>() {

        @Override
        public MemoryMappedDataChannel call() throws Exception {
          try (RandomAccessFile rf = new RandomAccessFile(filename, "rw")) {
            try (FileChannel channel = rf.getChannel()) {
              final MappedByteBuffer buffer =
                  channel.map(FileChannel.MapMode.READ_WRITE, part.getOffset(), part.getPartSize());
              MemoryMappedDataChannel memoryChannel =
                  new MemoryMappedDataChannel(buffer, part.getOffset(), part.getPartSize(), null);
              if (part.isCompleted()) {
                if (isCorrupted(memoryChannel, part, outputDir)) {
                  proxy.downloadPart(memoryChannel, part, objectId, outputDir);
                }
                progress.updateChecksum(1);
              } else {
                proxy.downloadPart(memoryChannel, part, objectId, outputDir);
                progress.updateProgress(1);
              }
              progress.incrementByteRead(part.getPartSize());
              memory.addAndGet(part.getPartSize());
              return memoryChannel;
            }
          }
        }
      }));
      progress.incrementByteWritten(part.getPartSize());
      long remaining = memory.addAndGet(-part.getPartSize());
      log.debug("Remaining Memory : {}", remaining);
      if (memory.get() < 0L || shouldThrottled) {
        shouldThrottled = true;
        try {
          Future<MemoryMappedDataChannel> work = results.remove();
          log.debug("Garbage collection starts");
          work.get().close();
        } catch (ExecutionException e) {
          log.error("Download part failed", e);
          hasError = true;
        }
        System.gc();
        log.debug("Garbage collection ends");
      }
    }

    executor.shutdown();
    executor.awaitTermination(super.maxUploadDuration, TimeUnit.DAYS);
    if (hasError) {
      progress.end(true);
    } else {
      try {
        takeCareOfException(results);
      } catch (Throwable e) {
        progress.end(true);
        throw e;
      }
    }

    progress.end(false);

  }

  private long calculateTotalSize(List<Part> parts) {
    long total = 0;
    for (Part part : parts) {
      total += part.getPartSize();
    }
    return total;
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
