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
package org.icgc.dcc.storage.client.upload.s3;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.icgc.dcc.storage.client.cli.Terminal;
import org.icgc.dcc.storage.client.exception.NotResumableException;
import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.progress.Progress;
import org.icgc.dcc.storage.client.transport.StorageService;
import org.icgc.dcc.storage.client.transport.Transport;
import org.icgc.dcc.storage.client.transport.Transport.Mode;
import org.icgc.dcc.storage.client.upload.UploadService;
import org.icgc.dcc.storage.client.upload.UploadStateStore;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.core.model.UploadProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * main class to handle uploading objects
 */
@Slf4j
public class S3UploadService implements UploadService {

  /**
   * Configuration.
   */
  @Value("${client.quiet}")
  private boolean quiet;
  @Value("${storage.retryNumber}")
  private int retryNumber;

  /**
   * Dependencies.
   */
  @Autowired
  private StorageService storageService;
  @Autowired
  private Transport.Builder transportBuilder;
  @Autowired
  private Terminal terminal;

  @PostConstruct
  public void setup() {
    retryNumber = retryNumber < 0 ? Integer.MAX_VALUE : retryNumber;
  }

  /**
   * The only public method for client to call to upload data to remote storage
   * 
   * @param file The file to be uploaded
   * @param objectId The object id that is used to associate the file in the remote storage
   * @param redo If redo the upload is required
   * @throws IOException
   */
  @Override
  public void upload(File file, String objectId, String md5, final boolean redo) throws IOException {
    boolean tryAgain = redo;
    for (int retry = 0; retry < retryNumber; retry++)
      try {
        if (tryAgain) {
          startUpload(file, objectId, md5, tryAgain);
        } else {
          // only perform checksum the first time of the resume
          resumeIfPossible(file, objectId, md5, retry == 0 ? true : false);
        }
        return;
      } catch (NotRetryableException e) {
        log.warn(
            "Upload was not completed successfully in the last execution. Checking data integrity. Please wait...");
        tryAgain = !storageService.isUploadDataRecoverable(objectId, file.length());
      }
  }

  /**
   * Start an upload given the object id
   */
  @SneakyThrows
  private void startUpload(File file, String objectId, String md5, boolean overwrite) {
    log.info("Start a new upload...");
    ObjectSpecification spec = null;
    try {
      spec = storageService.initiateUpload(objectId, file.length(), overwrite, md5);
    } catch (NotRetryableException e) {
      // A NotRetryable exception during initiateUpload should just end whole process
      // a bit of a sleazy hack. Should only be thrown when the Metadata service informs us the supplied
      // object id was never registered/does not exist in Metadata repo
      throw new NotResumableException(e);
    }

    // Delete if already present
    if (overwrite) {
      UploadStateStore.create(file, spec, false);
    }

    val progress = new Progress(terminal, quiet, spec.getParts().size(), 0);
    uploadParts(spec.getParts(), file, objectId, spec.getUploadId(), progress);
    cleanupState(file, objectId);
  }

  /**
   * Resume a upload if it is possible. Otherwise, it will start a new upload. Resume might not be possible if the
   * upload progress cannot be retrieved.
   */
  @SneakyThrows
  private void resumeIfPossible(File uploadFile, String objectId, String md5, boolean checksum) {
    try {
      val progress = checkProgress(uploadFile, objectId);
      resume(uploadFile, progress, objectId, checksum);
    } catch (NotRetryableException e) {
      // org.icgc.dcc.storage.client.exception.ServiceRetryableResponseErrorHandler translates the 404 received from
      // server into a NotRetryableException
      log.info("No upload id found for object id {}. Start new upload.", objectId);
      startUpload(uploadFile, objectId, md5, true);
      return;
    }
  }

  @SneakyThrows
  private UploadProgress checkProgress(File uploadFile, String objectId) {

    // See if there is already an upload in progress for this object id. Fetch upload id and send if present. If
    // missing, send null
    val uploadId = UploadStateStore.fetchUploadId(uploadFile, objectId);
    val progress = storageService.getProgress(objectId, uploadFile.length());

    // Compare upload id's
    if (uploadId.isPresent() && progress != null) {
      if (!uploadId.get().equalsIgnoreCase(progress.getUploadId())) {
        // Can't continue; upload id's don't match
        val msg =
            String.format(
                "Local in-progress upload %s conflicts with remote upload in progress %s. Aborting local upload.",
                uploadId.get(),
                progress.getUploadId());
        throw new NotResumableException(new IllegalStateException(msg));
      }
      // Then local upload id not present
    } else if (progress != null) {
      val msg =
          String
              .format(
                  "There is already an upload in progress that was started elsewhere with upload id %s. Aborting local upload.",
                  progress.getUploadId());
      throw new NotResumableException(new IllegalStateException(msg));
    }

    return progress;
  }

  /**
   * Resume an upload given the upload progress. Checksum is required only for the first attempt for each process
   * execution.
   */
  private void resume(File file, UploadProgress uploadProgress, String objectId, boolean checksum) throws IOException {
    log.info("Resume from the previous upload...");

    val parts = uploadProgress.getParts();
    int completedParts = numCompletedParts(parts);
    int totalParts = parts.size();

    // Remove completed parts if don't require checksum-ing
    if (!checksum) {
      parts.removeIf((Part part) -> part.isCompleted());
    }

    if (!checksum) {
      parts.removeIf((Part part) -> part.isCompleted());
    }

    val progress = new Progress(terminal, quiet, totalParts, completedParts);
    uploadParts(parts, file, uploadProgress.getObjectId(), uploadProgress.getUploadId(), progress);
    cleanupState(file, objectId);
  }

  /**
   * Calculate the number of completed parts
   */
  private int numCompletedParts(List<Part> parts) {
    int completedTotal = 0;
    for (val part : parts) {
      if (part.getMd5() != null) {
        completedTotal++;
      }
    }
    return completedTotal;
  }

  /**
   * Start upload parts using a specific configured data transport
   */
  @SneakyThrows
  private void uploadParts(List<Part> parts, File file, String objectId, String uploadId, Progress progressBar) {
    val transport = transportBuilder
        .withProxy(storageService)
        .withProgressBar(progressBar)
        .withParts(parts)
        .withObjectId(objectId)
        .withTransportMode(Mode.UPLOAD)
        .withSessionId(uploadId).build();

    transport.send(file);
  }

  @Override
  public boolean isObjectExist(String objectId) throws IOException {
    return storageService.isObjectExist(objectId);
  }

  private void cleanupState(File uploadFile, String objectId) throws IOException {
    UploadStateStore.close(UploadStateStore.getContainingDir(uploadFile), objectId);
  }

}
