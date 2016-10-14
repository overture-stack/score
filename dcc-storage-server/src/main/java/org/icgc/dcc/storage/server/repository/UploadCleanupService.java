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
package org.icgc.dcc.storage.server.repository;

import static com.google.common.base.Strings.repeat;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.Instant;

import org.icgc.dcc.storage.core.util.ObjectKeys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.model.MultipartUpload;

import lombok.Setter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Service that will periodically remove aborted or failed uploads.
 */
@Slf4j
@Setter
@Service
@ConditionalOnProperty("upload.clean.enabled")
public class UploadCleanupService {

  /**
   * Configuration.
   */
  @Value("${collaboratory.data.directory}")
  private String dataDir;
  @Value("${collaboratory.upload.expiration}")
  private int expiration;

  /**
   * Dependencies.
   */
  @Autowired
  private UploadService uploadService;

  @Scheduled(cron = "${upload.clean.cron}")
  public void clean() {
    log.info(repeat("-", 100));
    log.info("Cleaning stale uploads");
    log.info(repeat("-", 100));

    log.info("In-progress multipart uploads: ");
    for (val upload : uploadService.listUploads()) {
      log.info(" - Upload: {}", formatUpload(upload));

      if (isStale(upload)) {
        log.info("*** Cancelling stale upload: {}", formatUpload(upload));

        try {
          val objectId = ObjectKeys.getObjectId(dataDir, upload.getKey());
          uploadService.cancelUpload(objectId, upload.getUploadId());
        } catch (Exception e) {
          log.error("Error cancelling stale upload: {}: {}", formatUpload(upload), e);
        }
      }
    }
  }

  private boolean isStale(MultipartUpload upload) {
    val started = upload.getInitiated().toInstant();
    val threshold = Instant.now().minus(expiration, DAYS);
    return started.isBefore(threshold);
  }

  private static String formatUpload(MultipartUpload upload) {
    return String.format("uploadId = %s, key = %s, initiated = %s, owner = %s, initiator = %s, storageClass = %s",
        upload.getUploadId(),
        upload.getKey(),
        upload.getInitiated(),
        upload.getOwner(),
        upload.getInitiator(),
        upload.getStorageClass());
  }

}
