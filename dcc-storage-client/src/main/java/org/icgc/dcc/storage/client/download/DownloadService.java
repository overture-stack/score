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
package org.icgc.dcc.storage.client.download;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.getOnlyElement;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.client.cli.Terminal;
import org.icgc.dcc.storage.client.exception.NotResumableException;
import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.progress.Progress;
import org.icgc.dcc.storage.client.transport.StorageService;
import org.icgc.dcc.storage.client.transport.Transport;
import org.icgc.dcc.storage.client.transport.Transport.Mode;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DownloadService {

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
  private DownloadStateStore downloadStateStore;
  @Autowired
  private Transport.Builder transportBuilder;
  @Autowired
  private Terminal terminal;

  @PostConstruct
  public void setup() {
    retryNumber = retryNumber < 0 ? Integer.MAX_VALUE : retryNumber;
  }

  @SneakyThrows
  public URL getUrl(@NonNull String objectId) {
    return getUrl(objectId, 0, -1);
  }

  @SneakyThrows
  public String getUrlAsString(@NonNull String objectId) {
    return getUrlAsString(objectId, 0, -1);
  }

  /**
   * This method returns a pre-signed URL for downloading the blob associated with the objectId S3 key.
   * @param objectId
   * @param offset
   * @param length
   * @return
   */
  @SneakyThrows
  public URL getUrl(@NonNull String objectId, long offset, long length) {
    ObjectSpecification spec = storageService.getExternalDownloadSpecification(objectId, offset, length);
    Part file = getOnlyElement(spec.getParts()); // Throws IllegalArgumentException if more than one part

    return new URL(file.getUrl());
  }

  @SneakyThrows
  public String getUrlAsString(@NonNull String objectId, long offset, long length) {
    ObjectSpecification spec = storageService.getExternalDownloadSpecification(objectId, offset, length);
    Part file = getOnlyElement(spec.getParts()); // Throws IllegalArgumentException if more than one part

    return file.getUrl();
  }

  /**
   * The only public method for client to call to download data from remote storage.
   * 
   * @param file The file to be uploaded
   * @param objectId The object id that is used to associate the file in the remote storage
   * @param force If true the upload is required
   * @throws IOException
   */
  public void download(File outputDirectory, String objectId, long offset, long length, boolean force)
      throws IOException {
    log.debug("Beginning download of {} to {} {} - {}", objectId, outputDirectory.toString(), offset, length);
    int retry = 0;
    for (; retry < retryNumber; retry++) {
      try {
        if (force) {
          // Create local file handle for output
          File objFile = Downloads.getDownloadFile(outputDirectory, objectId);
          if (objFile.exists()) {
            // Delete if already there
            checkState(objFile.delete());
          }
          startNewDownload(outputDirectory, objectId, offset, length);
        } else {
          // Only perform checksum the first time of the resume
          resumeIfPossible(outputDirectory, objectId, offset, length, retry == 0 ? true : false);
        }
        return;
      } catch (NotResumableException e) {
        log.error("Failed to handle download request", e.getCause());
        throw e;
      } catch (NotRetryableException e) {
        log.warn(
            "Download failed during last execution. Checking data integrity. Please wait...", e);
        if (storageService.isDownloadDataRecoverable(outputDirectory, objectId,
            Downloads.getDownloadFile(outputDirectory, objectId).length())) {
          force = true;
        } else {
          force = false;
        }
      }
    }
    if (retry == retryNumber) {
      throw new RuntimeException("Number of retries exhausted");
    }
  }

  private void resumeIfPossible(File outputDirectory, String objectId, long offset, long length, boolean checksum)
      throws IOException {
    log.debug("Attempting to resume download for {} to {} {} - {}", objectId, outputDirectory.toString(), offset,
        length);
    List<Part> parts = null;
    try {
      parts = downloadStateStore.getProgress(outputDirectory, objectId);
    } catch (NotRetryableException e) {
      log.info("New download: {} because {}", objectId, e.getMessage());
      startNewDownload(outputDirectory, objectId, offset, length);
      return;
    }
    resume(parts, outputDirectory, objectId, checksum);
  }

  private void resume(List<Part> parts, File outputDirectory, String objectId, boolean checksum) {
    log.info("Resuming from previous download...");

    int totalParts = parts.size();
    int completedParts = numCompletedParts(parts);
    int remainingParts = totalParts - completedParts;

    log.info("Total parts: {}, completed parts: {}, remaining parts: {}", totalParts, completedParts, remainingParts);
    val progress = new Progress(terminal, quiet, totalParts, completedParts);
    downloadParts(parts, outputDirectory, objectId, objectId, progress, checksum);

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
   * Calculate the total size of completed parts
   */
  @SuppressWarnings("unused")
  private long completedPartsUsedSpace(List<Part> parts) {
    long completedSize = 0;

    for (Part part : parts) {
      if (part.getMd5() != null) completedSize += part.getPartSize();
    }
    return completedSize;

  }

  /**
   * Computes space requirements for download and ensure there is sufficient space locally to store it
   */
  @SneakyThrows
  public long getSpaceRequired(Set<Entity> entities) {
    long total = 0L;

    for (val entity : entities) {
      ObjectSpecification spec = storageService.getDownloadSpecification(entity.getId());
      total += spec.getObjectSize();
    }

    return total;
  }

  /**
   * Start a download given the object id
   */
  @SneakyThrows
  private void startNewDownload(File dir, String objectId, long offset, long length) {
    log.info("Starting a new download...");
    File objFile = Downloads.getDownloadFile(dir, objectId);

    if (objFile.exists()) {
      throw new NotResumableException(new FileAlreadyExistsException(objFile.getPath()));
    }

    if (!dir.exists()) {
      log.debug("{} did not exist; creating now", dir.toString());
      Files.createDirectories(dir.toPath());
      log.debug("finished creating {}", dir.toString());
    }

    log.debug("Downloading specification for {}: {}-{}", objectId, offset, length);
    ObjectSpecification spec = storageService.getDownloadSpecification(objectId, offset, length);
    log.info("Finished retrieving download specification file");
    downloadStateStore.init(dir, spec);

    // TODO: Assign session id
    val progress = new Progress(terminal, quiet, spec.getParts().size(), 0);
    downloadParts(spec.getParts(), dir, objectId, objectId, progress, false);
  }

  /**
   * start downloading parts using a specific configured data transport
   */
  @SneakyThrows
  private void downloadParts(List<Part> parts, File file, String objectId, String sessionId, Progress progressBar,
      boolean checksum) {
    log.debug("Setting up download of parts");
    transportBuilder.withProxy(storageService)
        .withProgressBar(progressBar)
        .withParts(parts)
        .withObjectId(objectId)
        .withTransportMode(Mode.DOWNLOAD)
        .withChecksum(checksum)
        .withSessionId(sessionId);
    transportBuilder.build().receive(file);
  }

}
