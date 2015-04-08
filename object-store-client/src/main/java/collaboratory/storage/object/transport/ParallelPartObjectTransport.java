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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import collaboratory.storage.object.store.client.upload.NotRetryableException;
import collaboratory.storage.object.store.client.upload.ObjectUploadServiceProxy;
import collaboratory.storage.object.store.client.upload.ProgressBar;
import collaboratory.storage.object.store.core.model.Part;

import com.google.api.client.util.Preconditions;

@AllArgsConstructor
public class ParallelPartObjectTransport implements ObjectTransport {

  private static final int MIN_WORKER = 10;
  private static final long MIN_MEMORY = 1024 * 10 * 10 * 10 * 10;

  final private ObjectUploadServiceProxy proxy;
  final private int numberOfWorkerThread;
  final private ProgressBar progress;
  final private List<Part> parts;
  final private String objectId;
  final private String uploadId;
  final private long memory;

  private ParallelPartObjectTransport(ParallelBuilder builder) {

    this.proxy = builder.proxy;
    this.progress = builder.progressBar;
    this.parts = builder.parts;
    this.objectId = builder.objectId;
    this.uploadId = builder.uploadId;
    this.numberOfWorkerThread = builder.numberOfWorkerThreads;
    this.memory = builder.memory;
  }

  @Override
  @SneakyThrows
  public void send(File file) {
    ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkerThread);
    for (Part part : parts) {
      Future<?> result = executor.submit(new Runnable() {

        @Override
        public void run() {
          try {
            proxy.uploadPart(file, part, objectId, uploadId);
          } catch (IOException e) {
            throw new NotRetryableException();
          }
        }
      });
    }
    progress.updateProgress(1);
    proxy.finalizeUpload(objectId, uploadId);

  }

  private int getCapacity() {
    return 10;
  }

  public static class ParallelBuilder extends ObjectTransport.AbstractBuilder {

    private int numberOfWorkerThreads;
    private long memory;

    public Builder withNumberOfWorkerThreads(int threads) {
      this.numberOfWorkerThreads = threads;
      return this;
    }

    public Builder withMemory(int memory) {
      this.memory = memory;
      return this;
    }

    @Override
    public ObjectTransport build() {
      Preconditions.checkNotNull(parts);
      Preconditions.checkNotNull(proxy);
      Preconditions.checkNotNull(objectId);
      Preconditions.checkNotNull(uploadId);
      Preconditions.checkNotNull(progressBar);

      numberOfWorkerThreads = numberOfWorkerThreads < MIN_WORKER ? MIN_WORKER : numberOfWorkerThreads;
      memory = memory < MIN_MEMORY ? MIN_MEMORY : memory;

      return new ParallelPartObjectTransport(this);
    }
  }
}
