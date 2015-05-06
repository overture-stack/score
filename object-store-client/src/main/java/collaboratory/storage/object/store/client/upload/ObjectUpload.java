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
package collaboratory.storage.object.store.client.upload;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.transport.ObjectTransport;

@Slf4j
@Component
public class ObjectUpload {

  @Autowired
  private ObjectUploadServiceProxy proxy;
  @Autowired
  private ObjectTransport.Builder transportBuilder;

  @Value("${client.upload.retryNumber}")
  private int retryNumber;

  @PostConstruct
  public void setup() {
    retryNumber = retryNumber < 0 ? Integer.MAX_VALUE : retryNumber;
  }

  public void upload(File file, String objectId, boolean redo) throws IOException {
    for (int retry = 0; retry < retryNumber; retry++)
      try {
        if (redo) {
          startUpload(file, objectId);
        } else {
          // only perform checksum the first time of the resume
          resumeIfPossible(file, objectId, retry == 0 ? true : false);
        }
        return;
      } catch (NotRetryableException e) {
        // TODO: server side check data integrity, if data integrity is not recoverable (i.e. NotRetryable), startupload
        // again else try resume
        log.warn("Upload is not completed successfully in the last execution. Checking data integrity. Please wait...");
        redo = !proxy.isUploadDataRecoverable(objectId, file.length());
      }
  }

  @SneakyThrows
  private void startUpload(File file, String objectId) {
    log.info("Start a new upload...");
    UploadSpecification spec = proxy.initiateUpload(objectId, file.length());
    uploadParts(spec.getParts(), file, objectId, spec.getUploadId(), new ProgressBar(spec.getParts().size(), spec
        .getParts().size()));
  }

  @SneakyThrows
  private void resumeIfPossible(File file, String objectId, boolean checksum) {
    UploadProgress progress = null;
    try {
      progress = proxy.getProgress(objectId, file.length());
    } catch (NotRetryableException e) {
      log.info("New upload: {}", objectId);
      startUpload(file, objectId);
      return;
    }
    resume(file, progress, objectId, checksum);

  }

  private void resume(File file, UploadProgress progress, String objectId, boolean checksum) {
    log.info("Resume from the previous upload...");

    List<Part> parts = progress.getParts();
    int completedTotal = numCompletedParts(parts);
    int total = parts.size();

    // remove completed parts if don't require checksumming
    if (!checksum) {
      parts.removeIf(new Predicate<Part>() {

        @Override
        public boolean test(Part part) {
          return part.getMd5() != null;
        }
      });
    }

    uploadParts(parts, file, progress.getObjectId(), progress.getUploadId(), new ProgressBar(total, total
        - completedTotal));

  }

  private int numCompletedParts(List<Part> parts) {
    int completedTotal = 0;
    for (Part part : parts) {
      if (part.getMd5() != null) completedTotal++;
    }
    return completedTotal;

  }

  @SneakyThrows
  private void uploadParts(List<Part> parts, File file, String objectId, String uploadId, ProgressBar progressBar) {
    transportBuilder.withProxy(proxy)
        .withProgressBar(progressBar)
        .withParts(parts)
        .withObjectId(objectId)
        .withUploadId(uploadId);
    log.debug("Transport Configuration: {}", transportBuilder);
    transportBuilder.build().send(file);
  }
}
