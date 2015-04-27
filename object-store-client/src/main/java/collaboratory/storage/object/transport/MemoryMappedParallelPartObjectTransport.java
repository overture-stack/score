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
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import collaboratory.storage.object.store.client.upload.MemoryMappedInputChannel;
import collaboratory.storage.object.store.core.model.Part;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;

@Slf4j
public class MemoryMappedParallelPartObjectTransport extends RemoteParallelPartObjectTransport {

  private MemoryMappedParallelPartObjectTransport(RemoteParallelBuilder builder) {
    super(builder);
  }

  @Override
  @SneakyThrows
  public void send(File file) {
    log.debug("send file: {}", file.getPath());
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    ImmutableList.Builder<Future<Part>> results = ImmutableList.builder();
    // getMaximumReadSpeed(file);
    progress.start();
    for (final Part part : parts) {
      try (FileInputStream fis = new FileInputStream(file)) {
        final MappedByteBuffer buffer =
            fis.getChannel().map(FileChannel.MapMode.READ_ONLY, part.getOffset(), part.getPartSize());
        buffer.load();
        results.add(executor.submit(new Callable<Part>() {

          @Override
          public Part call() throws Exception {
            MemoryMappedInputChannel channel = new MemoryMappedInputChannel(buffer, 0, part.getPartSize(), null);
            if (part.getMd5() != null) {
              if (resent(channel, part)) {
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

  private static final long MIN_READ = 100 * 1024 * 1024;

  @SneakyThrows
  protected void getMaximumReadSpeed(File file) {
    Stopwatch watch = Stopwatch.createStarted();
    RandomAccessFile memoryMappedFile = new RandomAccessFile(file, "r");
    long bytesReadInBytes = file.length() > MIN_READ ? MIN_READ : file.length();
    MappedByteBuffer in =
        memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, bytesReadInBytes);
    WritableByteChannel writeChannel = Channels.newChannel(ByteStreams.nullOutputStream());
    writeChannel.write(in);
    watch.stop();
    System.err.println("Maximum Read Speed (B/s): " + bytesReadInBytes / watch.elapsed(TimeUnit.MILLISECONDS) * 1000);
  }
}
