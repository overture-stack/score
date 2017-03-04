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

import lombok.Setter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.UploadProgress;
import org.icgc.dcc.storage.core.util.ObjectKeys;
import org.icgc.dcc.storage.server.exception.InternalUnrecoverableError;
import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.icgc.dcc.storage.server.repository.PartCalculator;
import org.icgc.dcc.storage.server.repository.URLGenerator;
import org.icgc.dcc.storage.server.repository.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

@Slf4j
@Setter
@Service
@Profile("azure")
public class AzureUploadService implements UploadService {

  @Autowired
  private URLGenerator urlGenerator;
  @Autowired
  private CloudBlobContainer container;
  @Autowired
  @Qualifier("upload")
  public PartCalculator partCalculator;

  @Override
  public ObjectSpecification initiateUpload(String objectId, long fileSize, String md5, boolean overwrite) {
    ObjectSpecification result;
    try {
      val blob = container.getBlockBlobReference(objectId);

      if ((!overwrite) && blob.exists()) {
        val message = String.format("Attempted to overwrite object id %s", objectId);
        log.error(message); // Log overwrite attempt occurrence to audit log file
        throw new InternalUnrecoverableError(message);
      }

      val objectKey = ObjectKeys.getObjectKey("", objectId);

      val parts = partCalculator.specify(0, fileSize);
      val part = parts.get(0);

      // Container name pre-determined by AzureURLGenerator
      val presignedUrl = urlGenerator.getUploadPartUrl("", objectKey, "", part, null);
      part.setMd5(md5);
      part.setUrl(presignedUrl);

      // Construct SAS and wrap it up in an ObjectSpecification
      result = new ObjectSpecification(objectKey.getKey(), objectId, objectId, parts, fileSize, md5, false);
      System.out.println();
    } catch (URISyntaxException e) {
      throw new NotRetryableException(e);
    } catch (StorageException e) {
      throw new NotRetryableException(e);
    }
    return result;
  }

  @Override
  public boolean exists(String objectId) {
    try {
      val blob = container.getBlockBlobReference(objectId);
      return blob.exists();
    } catch (URISyntaxException e) {
      throw new NotRetryableException(e);
    } catch (StorageException e) {
      throw new NotRetryableException(e);
    }
  }

  @Override
  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String eTag) {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
  }

  @Override
  public void finalizeUpload(String objectId, String uploadId) {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
  }

  @Override
  public String getUploadId(String objectId) {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
    return "";
  }

  @Override
  public ObjectMetadata getObjectMetadata(String objectId) {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
    return null;
  }

  @Override
  public UploadProgress getUploadStatus(String objectId, String uploadId, long fileSize) {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
    return null;
  }

  @Override
  public void cancelUploads() {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
  }

  @Override
  public void cancelUpload(String objectId, String uploadId) {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
  }

  @Override
  public void recover(String objectId, long fileSize) {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
  }

  @Override
  public void deletePart(String objectId, String uploadId, int partNumber) {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
  }

  @Override
  public List<MultipartUpload> listUploads() {
    // Empty implementation - not applicable for Azure Upload since we're using the Upload implementation supplied by
    // SDK
    return Collections.<MultipartUpload> emptyList();
  }
}
