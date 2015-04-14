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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import collaboratory.storage.object.store.client.upload.FileInputChannel;
import collaboratory.storage.object.store.client.upload.ObjectUploadServiceProxy;
import collaboratory.storage.object.store.client.upload.ProgressBar;
import collaboratory.storage.object.store.core.model.Part;

import com.google.api.client.util.Preconditions;
import com.google.common.collect.ImmutableList;

@AllArgsConstructor
public class RemoteParallelPartObjectTransport implements ObjectTransport {

  private static final int MIN_WORKER = 10;
  private static final long MIN_MEMORY = 1024 * 1024;

  final protected ObjectUploadServiceProxy proxy;
  final protected int nThreads;
  final protected ProgressBar progress;
  final protected List<Part> parts;
  final protected String objectId;
  final protected String uploadId;
  final protected AtomicLong memory;
  final protected int maxUploadDuration;

  protected RemoteParallelPartObjectTransport(RemoteParallelBuilder builder) {

    this.proxy = builder.proxy;
    this.progress = builder.progressBar;
    this.parts = builder.parts;
    this.objectId = builder.objectId;
    this.uploadId = builder.uploadId;
    this.nThreads = builder.nThreads;
    this.memory = new AtomicLong(builder.memory);
    this.maxUploadDuration = builder.maxUploadDuration;
  }

  @Override
  @SneakyThrows
  public void send(File file) {
    // ExecutorService executor = new ThreadPoolExecutor(nThreads, nThreads,
    // 0L, TimeUnit.MILLISECONDS,
    // new ArrayBlockingQueue<Runnable>(getCapacity(), true));
    ExecutorService executor = Executors.newFixedThreadPool(nThreads);

    ImmutableList.Builder<Future<Part>> results = ImmutableList.builder();
    progress.start();
    for (final Part part : parts) {
      results.add(executor.submit(new Callable<Part>() {

        @Override
        public Part call() throws Exception {
          proxy.uploadPart(new FileInputChannel(file, part.getOffset(), part.getPartSize(), null), part,
              objectId, uploadId);
          progress.incrementByteWritten(part.getPartSize());
          progress.incrementByteRead(part.getPartSize());
          progress.updateProgress(1);
          return part;
        }
      }));
    }

    executor.shutdown();
    executor.awaitTermination(maxUploadDuration, TimeUnit.DAYS);
    try {
      takeCareOfException(results.build());
      proxy.finalizeUpload(objectId, uploadId);
    } finally {
      progress.end();
    }

  }

  protected void takeCareOfException(List<Future<Part>> results) throws Throwable {
    for (Future<Part> result : results) {
      try {
        result.get();
      } catch (ExecutionException e) {
        throw e.getCause();
      }
    }
  }

  protected void getReadSpeed() {

  }

  protected int getCapacity() {
    return 10;
  }

  public static RemoteParallelBuilder builder() {
    return new RemoteParallelBuilder();
  }

  public static class RemoteParallelBuilder extends ObjectTransport.AbstractBuilder {

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
    public ObjectTransport build() {
      checkArgumentsNotNull();
      return new RemoteParallelPartObjectTransport(this);
    }

    protected void checkArgumentsNotNull() {
      Preconditions.checkNotNull(parts);
      Preconditions.checkNotNull(proxy);
      Preconditions.checkNotNull(objectId);
      Preconditions.checkNotNull(uploadId);
      Preconditions.checkNotNull(progressBar);

      nThreads = nThreads < MIN_WORKER ? MIN_WORKER : nThreads;
      memory = memory < MIN_MEMORY ? MIN_MEMORY : memory;
      maxUploadDuration = maxUploadDuration < 1 ? Integer.MAX_VALUE : maxUploadDuration;

    }
  }
}
