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
package org.icgc.dcc.storage.client.transport;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.icgc.dcc.storage.client.download.Downloads;
import org.icgc.dcc.storage.client.progress.Progress;
import org.icgc.dcc.storage.client.progress.ProgressDataChannel;
import org.icgc.dcc.storage.core.model.DataChannel;
import org.icgc.dcc.storage.core.model.Part;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * The default transport for parallel upload
 */
@Slf4j
@AllArgsConstructor
public class ParallelPartObjectTransport implements Transport {

  private static final int MIN_WORKER = 1;
  private static final long MIN_MEMORY = 1024L * 1024L;

  final protected StorageService proxy;
  final protected int nThreads;
  final protected int queueSize;
  final protected Progress progress;
  final protected List<Part> parts;
  final protected String objectId;
  final protected String uploadId;
  final protected Mode mode;
  final protected AtomicLong memory;
  final protected int maxUploadDuration;
  final protected boolean checksum;

  protected ParallelPartObjectTransport(RemoteParallelBuilder builder) {

    this.proxy = builder.proxy;
    this.progress = builder.progressBar;
    this.parts = builder.parts;
    this.objectId = builder.objectId;
    this.uploadId = builder.uploadId;
    this.nThreads = builder.nThreads;
    this.queueSize = nThreads * 2;
    this.memory = new AtomicLong(builder.memory);
    this.maxUploadDuration = builder.maxUploadDuration;
    this.mode = builder.mode;
    this.checksum = builder.checksum;
  }

  @Override
  @SneakyThrows
  public void send(File file) {
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);

    ImmutableList.Builder<Future<Part>> results = ImmutableList.builder();
    progress.start();
    for (final Part part : parts) {
      results.add(executor.submit(new Callable<Part>() {

        @Override
        public Part call() throws Exception {
          DataChannel channel =
              new ProgressDataChannel(new FileDataChannel(file, part.getOffset(), part.getPartSize(), null), progress);
          if (part.isCompleted()) {
            if (isCorrupted(channel, part, file)) {
              progress.startTransfer();
              proxy.uploadPart(channel, part, objectId, uploadId);
            }
            progress.incrementChecksumParts();
          } else {
            progress.startTransfer();
            proxy.uploadPart(channel, part, objectId, uploadId);
            progress.incrementParts(1);
          }
          // progress.incrementByteWritten(part.getPartSize());
          // progress.incrementByteRead(part.getPartSize());
          return part;
        }
      }));
    }

    executor.shutdown();
    executor.awaitTermination(maxUploadDuration, TimeUnit.DAYS);
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
    long fileSize = Downloads.calculateTotalSize(parts);
    log.debug("downloading object id: {}, size:{}", objectId, fileSize);
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    ImmutableList.Builder<Future<Part>> results = ImmutableList.builder();

    // This is used to calculate
    if (!Ordering.natural().isOrdered(parts)) {
      Collections.sort(parts);
    }

    progress.start();
    for (final Part part : parts) {
      results.add(executor.submit(new Callable<Part>() {

        @Override
        public Part call() throws Exception {
          DataChannel channel =
              new ProgressDataChannel(
                  new FileDataChannel(getPartFile(outputDir, part), part.getOffset(), part.getPartSize(), null),
                  progress);

          if (part.isCompleted()) {
            if (checksum && isCorrupted(channel, part, outputDir)) {
              progress.startTransfer();
              proxy.downloadPart(channel, part, objectId, outputDir);
            }
            progress.incrementChecksumParts();
          } else {
            progress.startTransfer();
            proxy.downloadPart(channel, part, objectId, outputDir);
            progress.incrementParts(1);
          }
          // progress.incrementByteRead(part.getPartSize());
          // progress.incrementByteWritten(part.getPartSize());
          return part;
        }
      }));
    }

    executor.shutdown();
    executor.awaitTermination(maxUploadDuration, TimeUnit.DAYS);

    try {
      mergeToFile(parts, outputDir);
    } catch (IOException e) {
      log.error("Fail to create the final output file for object-id {}", objectId, e);
      throw new Error(e);
    }

    progress.stop();
    try {
      takeCareOfException(results.build());
      proxy.finalizeDownload(outputDir, objectId);
      try {
        cleanup(parts, outputDir);
      } catch (Throwable e) {
        // No rethrow because the download is successful but deleting the part files are not
        log.warn("Fail to clean up part files for object id: {} at path: {}", objectId, outputDir.getAbsolutePath());
        log.warn("Please delete the temporary files at {}", outputDir.getAbsolutePath());
      }
    } catch (Throwable e) {
      progress.end(true);
      throw e;
    }
    progress.end(false);
  }

  private void cleanup(List<Part> parts, File outputDir) {
    for (val part : parts) {
      val partFile = getPartFile(outputDir, part);
      checkState(partFile.delete());
    }
  }

  // TODO: need unit test confirming case where source MD5 is null
  protected boolean isCorrupted(DataChannel channel, Part part, File outputDir) throws IOException {
    if ((part.getSourceMd5() != null) && channel.verifyMd5(part.getSourceMd5())) {
      return false;
    }
    log.debug("Part is corrupted: {}", part);

    switch (mode) {
    case UPLOAD:
      proxy.deleteUploadPart(objectId, uploadId, part);
      break;
    case DOWNLOAD:
      proxy.deleteDownloadPart(outputDir, objectId, part);

    }
    channel.reset();
    return true;
  }

  protected <T> void takeCareOfException(Collection<Future<T>> results) throws Throwable {
    for (Future<T> result : results) {
      try {
        result.get();
      } catch (ExecutionException e) {
        log.debug("Fail on the part", e.getCause());
        throw e.getCause();
      }
    }
  }

  protected int getCapacity() {
    return 10;
  }

  public static RemoteParallelBuilder builder() {
    return new RemoteParallelBuilder();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class RemoteParallelBuilder extends Transport.AbstractBuilder {

    private int nThreads;
    private long memory;
    private int maxUploadDuration;

    public RemoteParallelBuilder withNumberOfWorkerThreads(int threads) {
      this.nThreads = threads;
      return this;
    }

    public RemoteParallelBuilder withMemory(long memory) {
      this.memory = memory;
      return this;
    }

    public RemoteParallelBuilder withMaximumUploadDuration(int duration) {
      this.maxUploadDuration = duration;
      return this;
    }

    @Override
    public Transport build() {
      checkArgumentsNotNull();
      return new ParallelPartObjectTransport(this);
    }

    protected void checkArgumentsNotNull() {
      Preconditions.checkNotNull(parts);
      Preconditions.checkNotNull(proxy);
      Preconditions.checkNotNull(objectId);
      Preconditions.checkNotNull(uploadId);
      Preconditions.checkNotNull(progressBar);
      Preconditions.checkNotNull(mode);

      nThreads = nThreads < MIN_WORKER ? MIN_WORKER : nThreads;
      memory = memory < MIN_MEMORY ? MIN_MEMORY : memory;
      maxUploadDuration = maxUploadDuration < 1 ? Integer.MAX_VALUE : maxUploadDuration;

    }
  }

  private void mergeToFile(List<Part> parts, File outputDir) throws IOException {
    // appending parts to objectFile
    try (RandomAccessFile fos = new RandomAccessFile(Downloads.getDownloadFile(outputDir, objectId), "rw")) {
      fos.setLength(Downloads.calculateTotalSize(parts));
      FileChannel target = fos.getChannel();
      for (Part part : parts) {
        File partFile = getPartFile(outputDir, part);
        try (FileInputStream fis = new FileInputStream(partFile)) {
          fis.getChannel().transferTo(0, partFile.length(), target);
        }
      }
    }
  }

  @SneakyThrows
  private File getPartFile(File outDir, Part part) {
    return new File(outDir, "." + objectId + "-" + String.valueOf(part.getPartNumber()));
  }

}
