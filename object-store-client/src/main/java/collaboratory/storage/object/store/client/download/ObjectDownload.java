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
package collaboratory.storage.object.store.client.download;

import static com.google.common.collect.Iterables.getOnlyElement;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import collaboratory.storage.object.store.client.exception.NotResumableException;
import collaboratory.storage.object.store.client.exception.NotRetryableException;
import collaboratory.storage.object.store.client.transport.ObjectStoreServiceProxy;
import collaboratory.storage.object.store.client.transport.ObjectTransport;
import collaboratory.storage.object.store.client.transport.ObjectTransport.Mode;
import collaboratory.storage.object.store.client.transport.ProgressBar;
import collaboratory.storage.object.store.core.model.ObjectSpecification;
import collaboratory.storage.object.store.core.model.Part;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * main class to handle uploading objects
 */
@Slf4j
@Component
public class ObjectDownload {

  @Autowired
  private ObjectStoreServiceProxy proxy;

  @Autowired
  private DownloadStateStore downloadStateStore;

  @Autowired
  private ObjectTransport.Builder transportBuilder;

  @Value("${client.upload.retryNumber}")
  private int retryNumber;

  @PostConstruct
  public void setup() {
    retryNumber = retryNumber < 0 ? Integer.MAX_VALUE : retryNumber;
  }

  @SneakyThrows
  public URL getUrl(@NonNull String objectId) {
    val spec = proxy.getDownloadSpecification(objectId, 0, -1);
    val file = getOnlyElement(spec.getParts());

    return new URL(file.getUrl());
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
          File objFile = DownloadUtils.getDownloadFile(outputDirectory, objectId);
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
        log.error("Fail to handle download request", e.getCause());
        throw e;
      } catch (NotRetryableException e) {
        log.warn(
            "Download is not completed successfully in the last execution. Checking data integrity. Please wait...", e);
        if (proxy.isDownloadDataRecoverable(outputDirectory, objectId,
            DownloadUtils.getDownloadFile(outputDirectory, objectId).length())) {
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
      log.info("New download: {}", objectId, e);
      startNewDownload(outputDirectory, objectId, offset, length);
      return;
    }
    resume(parts, outputDirectory, objectId, checksum);
  }

  private void resume(List<Part> parts, File outputDirectory, String objectId, boolean checksum) {
    log.info("Resume from previous download...");

    int completedTotal = numCompletedParts(parts);
    int total = parts.size();

    downloadParts(parts, outputDirectory, objectId, objectId, new ProgressBar(total, total
        - completedTotal), checksum);

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
   * Start a upload given the object id
   */
  @SneakyThrows
  private void startNewDownload(File dir, String objectId, long offset, long length) {
    log.info("Start a new download...");
    File objFile = DownloadUtils.getDownloadFile(dir, objectId);
    if (objFile.exists()) {
      throw new NotResumableException(new FileAlreadyExistsException(objFile.getPath()));
    }
    if (!dir.exists()) {
      Files.createDirectories(dir.toPath());
    }
    ObjectSpecification spec = proxy.getDownloadSpecification(objectId, offset, length);
    downloadStateStore.init(dir, spec);
    // TODO: assign session id
    downloadParts(spec.getParts(), dir, objectId, objectId, new ProgressBar(spec.getParts().size(), spec
        .getParts().size()), false);
  }

  /**
   * start upload parts using a specific configured data transport
   */
  @SneakyThrows
  private void downloadParts(List<Part> parts, File file, String objectId, String sessionId, ProgressBar progressBar,
      boolean checksum) {
    transportBuilder.withProxy(proxy)
        .withProgressBar(progressBar)
        .withParts(parts)
        .withObjectId(objectId)
        .withTransportMode(Mode.DOWNLOAD)
        .withChecksum(checksum)
        .withSessionId(sessionId);
    transportBuilder.build().receive(file);
  }

}
