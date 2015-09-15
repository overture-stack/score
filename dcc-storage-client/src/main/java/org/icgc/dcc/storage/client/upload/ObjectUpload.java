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
package org.icgc.dcc.storage.client.upload;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.client.exception.NotResumableException;
import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.transport.ObjectStoreServiceProxy;
import org.icgc.dcc.storage.client.transport.ObjectTransport;
import org.icgc.dcc.storage.client.transport.ProgressBar;
import org.icgc.dcc.storage.client.transport.ObjectTransport.Mode;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.core.model.UploadProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * main class to handle uploading objects
 */
@Slf4j
@Component
public class ObjectUpload {

  @Autowired
  private ObjectStoreServiceProxy proxy;
  @Autowired
  private ObjectTransport.Builder transportBuilder;

  @Value("${client.upload.retryNumber}")
  private int retryNumber;

  @PostConstruct
  public void setup() {
    retryNumber = retryNumber < 0 ? Integer.MAX_VALUE : retryNumber;
  }

  /**
   * the only public method for client to call to upload data to collaboratory
   * 
   * @param file The file to be uploaded
   * @param objectId The object id that is used to associate the file in the collaboratory
   * @param redo If redo the upload is required
   * @throws IOException
   */
  public void upload(File file, String objectId, boolean redo) throws IOException {
    for (int retry = 0; retry < retryNumber; retry++)
      try {
        if (redo) {
          startUpload(file, objectId, redo);
        } else {
          // only perform checksum the first time of the resume
          resumeIfPossible(file, objectId, retry == 0 ? true : false);
        }
        return;
      } catch (NotRetryableException e) {
        log.warn("Upload is not completed successfully in the last execution. Checking data integrity. Please wait...");
        redo = !proxy.isUploadDataRecoverable(objectId, file.length());
      }
  }

  /**
   * Start a upload given the object id
   */
  @SneakyThrows
  private void startUpload(File file, String objectId, boolean overwrite) {
    log.info("Start a new upload...");
    ObjectSpecification spec = null;
    try {
      spec = proxy.initiateUpload(objectId, file.length(), overwrite);
    } catch (NotRetryableException e) {
      // A NotRetryable exception during initiateUpload should just end whole process
      // a bit of a sleazy hack. Should only be thrown when the Metadata service informs us the supplied
      // object id was never registered/does not exist in Metadata repo
      throw new NotResumableException(e);
    }
    uploadParts(spec.getParts(), file, objectId, spec.getUploadId(), new ProgressBar(spec.getParts().size(), spec
        .getParts().size()));
  }

  /**
   * Resume a upload if it is possible. Otherwise, it will start a new upload. Resume might not be possible if the
   * upload progress cannot be retrieved.
   */
  @SneakyThrows
  private void resumeIfPossible(File file, String objectId, boolean checksum) {
    UploadProgress progress = null;
    try {
      progress = proxy.getProgress(objectId, file.length());
    } catch (NotRetryableException e) {
      log.info("New upload: {}", objectId);
      startUpload(file, objectId, true);
      return;
    }
    resume(file, progress, objectId, checksum);

  }

  /**
   * Resume a upload given the upload progress. Checksum is required only for the first attempt for each process
   * execution.
   */
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
          return part.isCompleted();
        }
      });
    }

    uploadParts(parts, file, progress.getObjectId(), progress.getUploadId(), new ProgressBar(total, total
        - completedTotal));

  }

  /**
   * Calculate the number of completed parts
   */
  private int numCompletedParts(List<Part> parts) {
    int completedTotal = 0;
    for (Part part : parts) {
      if (part.getMd5() != null) completedTotal++;
    }
    return completedTotal;

  }

  /**
   * start upload parts using a specific configured data transport
   */
  @SneakyThrows
  private void uploadParts(List<Part> parts, File file, String objectId, String uploadId, ProgressBar progressBar) {
    transportBuilder.withProxy(proxy)
        .withProgressBar(progressBar)
        .withParts(parts)
        .withObjectId(objectId)
        .withTransportMode(Mode.UPLOAD)
        .withSessionId(uploadId);
    transportBuilder.build().send(file);
  }

  public boolean isObjectExist(String oid) throws IOException {
    return proxy.isObjectExist(oid);
  }
}
