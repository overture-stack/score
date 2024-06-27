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
package bio.overture.score.server.repository.azure;

import static com.google.common.base.Preconditions.checkArgument;

import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.core.model.Part;
import bio.overture.score.core.util.ObjectKeys;
import bio.overture.score.core.util.PartCalculator;
import bio.overture.score.server.exception.IdNotFoundException;
import bio.overture.score.server.exception.InternalUnrecoverableError;
import bio.overture.score.server.exception.NotRetryableException;
import bio.overture.score.server.repository.BucketNamingService;
import bio.overture.score.server.repository.DownloadService;
import bio.overture.score.server.repository.URLGenerator;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Setter
@Service
@Profile("azure")
public class AzureDownloadService implements DownloadService {

  /** Dependencies. */
  @Autowired private BucketNamingService bucketNamingService;

  @Autowired private URLGenerator urlGenerator;

  @Autowired
  @Qualifier("download")
  private PartCalculator partCalculator;

  @Autowired private CloudBlobContainer container;

  @Value("${object.sentinel}")
  private String sentinelObjectId;

  @Override
  public ObjectSpecification download(
      String objectId, long offset, long length, boolean forExternalUse, boolean excludeUrls) {
    try {
      checkArgument(offset >= 0L);

      val blob = getBlobReference(objectId); // checks for existence
      val blobSize = blob.getProperties().getLength();

      // Calculate range values
      // To retrieve to the end of the file
      long rangeLength = length;
      if (!forExternalUse && (rangeLength < 0L)) {
        rangeLength = blobSize - offset;
      }

      // Validate offset and length parameters:
      // Check if the offset + length > length - that would be too big
      if ((offset + rangeLength) > blobSize) {
        throw new InternalUnrecoverableError(
            String.format(
                "Specified parameters offset: %d length: %d exceed object size %d (object id: %s)",
                offset, rangeLength, blobSize, objectId));
      }

      List<Part> parts;
      if (forExternalUse) {
        // Return as a single part
        parts = partCalculator.specify(0L, blobSize);
      } else {
        parts = partCalculator.divide(offset, rangeLength);
      }
      fillPartUrls(objectId, parts);

      String contentMd5 = blob.getProperties().getContentMD5();
      String md5 = null;
      if (Objects.nonNull(contentMd5)) {
        md5 = base64ToHexMD5(contentMd5);
      }

      return new ObjectSpecification(objectId, objectId, objectId, parts, rangeLength, md5, false);
    } catch (StorageException e) {
      log.error(
          "Failed to download objectId: {}, offset: {}, length: {}, forExternalUse: {}: {} ",
          objectId,
          offset,
          length,
          forExternalUse,
          e);

      throw new NotRetryableException(e);
    } catch (URISyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  public CloudBlockBlob getBlobReference(String objectId)
      throws URISyntaxException, StorageException {
    val result = container.getBlockBlobReference(objectId);
    if (result.exists()) {
      return result;
    } else {
      throw new IdNotFoundException(
          String.format("Object '%s' not found in container '%s'", objectId, container.getName()));
    }
  }

  private void fillPartUrls(String objectId, List<Part> parts) {
    // Construct pre-signed URL's for objects - the same for all parts in Azure. Parts determined
    // entirely by range
    // header
    val presignedUrl =
        urlGenerator.getDownloadUrl(null, ObjectKeys.getObjectKey("", objectId), null);
    for (val part : parts) {
      part.setUrl(presignedUrl);
    }
  }

  @Override
  public String getSentinelObject() {
    if ((sentinelObjectId == null) || (sentinelObjectId.isEmpty())) {
      throw new NotRetryableException(
          new IllegalArgumentException("Sentinel object id not defined"));
    }
    val result =
        urlGenerator.getDownloadUrl(null, ObjectKeys.getObjectKey("", sentinelObjectId), null);
    return result;
  }

  @SneakyThrows
  static String base64ToHexMD5(String content) {
    val bytes = Base64.getDecoder().decode(content);
    val output = new StringBuilder();
    for (val b : bytes) {
      output.append(String.format("%02x", b));
    }

    log.trace("Converted MD5 from {} to {}", content, output);
    return output.toString();
  }
}
