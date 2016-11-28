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
package org.icgc.dcc.storage.server.repository.azure;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.icgc.dcc.storage.core.model.ObjectKey;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.icgc.dcc.storage.server.repository.URLGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;

/**
 * Amazon specific: To generate presigned url for s3-like object storage
 */
@Slf4j
public class AzureURLGenerator implements URLGenerator {

  // Formatter just for local logging/display
  private static final DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZone(
      ZoneId.systemDefault());

  @Value("${bucket.policy.upload}")
  private String uploadPolicy;
  @Value("${bucket.policy.download}")
  private String downloadPolicy;
  @Value("${collaboratory.download.expiration}")
  private int expiration;

  @Autowired
  private CloudBlobContainer azureContainer;

  /**
   * Ignore bucketName, uploadId, part and expiration parameters
   */
  @Override
  public String getUploadPartUrl(String bucketName, ObjectKey objectKey, String uploadId, Part part, Date expiration) {
    return generatePresignedUrl(objectKey.getObjectId(), uploadPolicy);
  }

  @Override
  public String getDownloadPartUrl(String bucketName, ObjectKey objectKey, Part part, Date expiration) {
    return getDownloadUrl(bucketName, objectKey, expiration);
  }

  @Override
  public String getDownloadUrl(String bucketName, ObjectKey objectKey, Date expiration) {
    return generatePresignedUrl(objectKey.getObjectId(), downloadPolicy);
  }

  /**
   * Presigned URL for Azure is really simple.
   * 
   * @param objectId
   * @return
   */
  protected String generatePresignedUrl(String objectId, String policyIdentifier) {
    try {
      val blob = azureContainer.getBlockBlobReference(objectId);

      String sasToken = blob.generateSharedAccessSignature(createBlobPolicy(), policyIdentifier);
      String sasUri = String.format("%s?%s", blob.getUri(), sasToken);
      return sasUri;
    } catch (URISyntaxException | StorageException e) {
      log.error("Unable to generate pre-signed download URL for '{}' due to '{}'", objectId, e.getMessage());
      throw new NotRetryableException(e);
    } catch (InvalidKeyException e) {
      log.error(e.getMessage());
      throw new NotRetryableException(e);
    }
  }

  public SharedAccessBlobPolicy createBlobPolicy() {
    SharedAccessBlobPolicy itemPolicy = new SharedAccessBlobPolicy();
    itemPolicy.setSharedAccessStartTime(getStartDate());
    itemPolicy.setSharedAccessExpiryTime(getExpirationDate());
    return itemPolicy;
  }

  /*
   * Applicable as of 15 minutes ago
   */
  private Date getStartDate() {
    val now = LocalDateTime.now();
    val result = now.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant();
    log.debug(String.format("URL valid from: %s", df.format(result)));
    return Date.from(result);
  }

  private Date getExpirationDate() {
    val now = LocalDateTime.now();
    val result = now.plusDays(expiration).atZone(ZoneId.systemDefault()).toInstant();
    log.debug(String.format("URL valid until: %s", df.format(result)));
    return Date.from(result);
  }

}
