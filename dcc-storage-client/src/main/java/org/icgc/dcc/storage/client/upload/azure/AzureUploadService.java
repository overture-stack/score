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
package org.icgc.dcc.storage.client.upload.azure;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.icgc.dcc.storage.client.cli.Terminal;
import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.progress.Progress;
import org.icgc.dcc.storage.client.transport.StorageService;
import org.icgc.dcc.storage.client.upload.UploadService;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.microsoft.azure.storage.AccessCondition;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.ResponseReceivedEvent;
import com.microsoft.azure.storage.RetryingEvent;
import com.microsoft.azure.storage.StorageEvent;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

@Slf4j
public class AzureUploadService implements UploadService {

  @Autowired
  private StorageService storageService;
  @Autowired
  private Terminal terminal;

  @Value("${transport.parallel}")
  private int parallelUploads;

  // This is Azure's maximum 4 MB upload part size
  private final static int BLOCK_SIZE = 4 * 1024 * 1024;

  @Override
  public void upload(File file, String objectId, String md5, boolean redo) throws IOException {
    // Get object specification from server
    val spec = storageService.initiateUpload(objectId, file.length(), redo, md5);

    // Calculate expected number of parts to track progress against
    val partInfo = calculateNumBlocks(file.length(), BLOCK_SIZE);

    // Will contain one Part as far as Storage Service is concerned. There is no need for more than one SAS to be
    // generated for an entire file; each Part does not require its own pre-signed URL.
    if (spec.getParts().isEmpty()) {
      throw new NotRetryableException(new Exception(
          "Did not get a valid ObjectSpecification from service: missing Part definition."));
    }

    val sasUrl = extractSAS(spec);

    try {
      val blob = new CloudBlockBlob(sasUrl);
      val progress = new Progress(terminal, false, Math.toIntExact(partInfo.getLeft()), 0);
      val ctx = new OperationContext();

      // Keep track of number of parts completed - we're also going to get an event for the finalize block list call
      val completedParts = new AtomicInteger();

      // Add listener for every completed call to Azure Blob Storage
      // This is how we track progress
      ctx.getResponseReceivedEventHandler().addListener(new StorageEvent<ResponseReceivedEvent>() {

        @Override
        public void eventOccurred(ResponseReceivedEvent eventArg) {
          long bytesSent = BLOCK_SIZE;
          int partCount = completedParts.incrementAndGet();

          if (partCount <= partInfo.getLeft()) {
            progress.incrementParts(1);
            progress.incrementBytesWritten(partCount == partInfo.getLeft() ? bytesSent = partInfo.getRight() : bytesSent);
          }
          // Don't increment if all parts have been accounted for (this is the Put Block List call)
          // This would result in progress bar reporting "Parts: 42/41"
        }
      });

      ctx.getResponseReceivedEventHandler().addListener(new StorageEvent<ResponseReceivedEvent>() {

        @Override
        public void eventOccurred(ResponseReceivedEvent eventArg) {
          long bytesSent = BLOCK_SIZE;
          int partCount = completedParts.incrementAndGet();

          if (partCount <= partInfo.getLeft()) {
            progress.incrementParts(1);
            progress.incrementBytesWritten(partCount == partInfo.getLeft() ? bytesSent = partInfo.getRight() : bytesSent);
          }
          // Don't increment if all parts have been accounted for (this is the Put Block List call)
          // This would result in progress bar reporting "Parts: 42/41"
        }
      });

      // Add listener for every retry attempt - just for informational purposes
      ctx.getRetryingEventHandler().addListener(new StorageEvent<RetryingEvent>() {

        @Override
        public void eventOccurred(RetryingEvent eventArg) {
          log.info("Upload required a retry - just saying");
        }
      });

      val options = new BlobRequestOptions();
      options.setConcurrentRequestCount(parallelUploads);

      progress.start();
      progress.startTransfer();
      blob.uploadFromFile(file.getAbsolutePath(), new AccessCondition(), options, ctx);
      progress.stop();
      progress.end(false);
    } catch (StorageException e) {
      throw new NotRetryableException(e);
    }
  }

  @Override
  public boolean isObjectExist(String objectId) throws IOException {
    return storageService.isObjectExist(objectId);
  }

  protected Pair<Integer, Long> calculateNumBlocks(long fileSize, long blockSize) {
    int partCount = Math.toIntExact(fileSize / blockSize);
    long rem = Math.floorMod(fileSize, blockSize);
    if (rem > 0L) {
      partCount += 1;
    }
    return Pair.of(partCount, rem);
  }

  /**
   * Get SAS URL from an ObjectSpecification
   * @param spec - ObjectSpecification with a Parts collection containing only a single part.
   * @return
   */
  protected URI extractSAS(ObjectSpecification spec) {
    try {
      return new URI(spec.getParts().get(0).getUrl());
    } catch (URISyntaxException e) {
      // Unrecognizable SAS URL
      throw new NotRetryableException(e);
    }
  }

}
