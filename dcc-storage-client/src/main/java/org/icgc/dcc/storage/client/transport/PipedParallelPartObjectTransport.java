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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.icgc.dcc.storage.core.model.Part;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

/**
 * A data transport using piped input channels for parallel upload
 */
@Slf4j
public class PipedParallelPartObjectTransport extends ParallelPartObjectTransport {

  private PipedParallelPartObjectTransport(RemoteParallelBuilder builder) {
    super(builder);
  }

  @Override
  @SneakyThrows
  public void send(File file) {

    log.debug("Number of Concurrency: {}", nThreads);
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);
    ImmutableList.Builder<Future<Part>> results = ImmutableList.builder();
    progress.start();
    for (final Part part : parts) {
      final PipedOutputStream pos = new PipedOutputStream();
      final PipedInputStream pis = new PipedInputStream(pos, (int) part.getPartSize());

      results.add(executor.submit(new Callable<Part>() {

        @Override
        public Part call() throws Exception {
          proxy.uploadPart(new PipedDataChannel(pis, 0, part.getPartSize(), null), part, objectId, uploadId);
          progress.incrementByteWritten(part.getPartSize());
          progress.updateProgress(1);
          memory.addAndGet(part.getPartSize());
          return part;
        }
      }));

      ByteSource source = Files.asByteSource(file);
      source.slice(part.getOffset(), part.getPartSize()).copyTo(pos);
      pos.close();
      progress.incrementByteRead(part.getPartSize());
      progress.updateProgress(0);
      long remaining = memory.addAndGet(-part.getPartSize());
      log.debug("Remaining Memory : {}", remaining);
      while (memory.get() < 0) {
        TimeUnit.SECONDS.sleep(1);
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

  public static LocalParallelBuilder builder() {
    return new LocalParallelBuilder();
  }

  public static class LocalParallelBuilder extends RemoteParallelBuilder {

    @Override
    public ObjectTransport build() {
      checkArgumentsNotNull();
      return new PipedParallelPartObjectTransport(this);
    }

  }

}
