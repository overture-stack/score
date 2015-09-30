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
package org.icgc.dcc.storage.client.download;

import static com.google.common.collect.Iterables.getOnlyElement;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.List;

import javax.annotation.PostConstruct;

import org.icgc.dcc.storage.client.cli.Terminal;
import org.icgc.dcc.storage.client.exception.NotResumableException;
import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.progress.Progress;
import org.icgc.dcc.storage.client.transport.StorageService;
import org.icgc.dcc.storage.client.transport.Transport;
import org.icgc.dcc.storage.client.transport.Transport.Mode;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * main class to handle uploading objects
 */
@Slf4j
@Component
public class DownloadService {

  @Autowired
  private StorageService storageService;
  @Autowired
  private DownloadStateStore downloadStateStore;
  @Autowired
  private Transport.Builder transportBuilder;
  @Autowired
  private Terminal terminal;

  @Value("${client.upload.retryNumber}")
  private int retryNumber;

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
    Part file = getOnlyElement(spec.getParts()); // throws IllegalArgumentException if more than one part

    return new URL(file.getUrl());
  }

  @SneakyThrows
  public String getUrlAsString(@NonNull String objectId, long offset, long length) {
    ObjectSpecification spec = storageService.getExternalDownloadSpecification(objectId, offset, length);
    Part file = getOnlyElement(spec.getParts()); // throws IllegalArgumentException if more than one part

    return file.getUrl();
  }

  /**
   * the only public method for client to call to download data to collaboratory
   * 
   * @param file The file to be uploaded
   * @param objectId The object id that is used to associate the file in the collaboratory
   * @param redo If redo the upload is required
   * @throws IOException
   */
  public void download(File outputDirectory, String objectId, long offset, long length, boolean redo)
      throws IOException {
    int retry = 0;
    for (; retry < retryNumber; retry++) {
      try {
        if (redo) {
          File objFile = Downloads.getDownloadFile(outputDirectory, objectId);
          if (objFile.exists()) {
            objFile.delete();
          }
          startNewDownload(outputDirectory, objectId, offset, length);
        } else {
          // only perform checksum the first time of the resume
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
          redo = false;
        } else {
          redo = true;
        }
      }
    }
    if (retry == retryNumber) {
      throw new RuntimeException("Number of retries exhausted");
    }
  }

  private void resumeIfPossible(File outputDirectory, String objectId, long offset, long length, boolean checksum)
      throws IOException {
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
    log.info("Resume from previous download...");

    int completedTotal = numCompletedParts(parts);
    int total = parts.size();

    log.info("{} parts remaining", total);
    val progress = new Progress(total, total - completedTotal, terminal);
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
      Files.createDirectories(dir.toPath());
    }

    ObjectSpecification spec = storageService.getDownloadSpecification(objectId, offset, length);
    downloadStateStore.init(dir, spec);

    // TODO: Assign session id
    val progress = new Progress(spec.getParts().size(), spec.getParts().size(), terminal);
    downloadParts(spec.getParts(), dir, objectId, objectId, progress, false);
  }

  /**
   * start downloading parts using a specific configured data transport
   */
  @SneakyThrows
  private void downloadParts(List<Part> parts, File file, String objectId, String sessionId, Progress progressBar,
      boolean checksum) {
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
