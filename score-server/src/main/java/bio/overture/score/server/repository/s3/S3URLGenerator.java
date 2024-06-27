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
package bio.overture.score.server.repository.s3;

import bio.overture.score.core.model.ObjectKey;
import bio.overture.score.core.model.Part;
import bio.overture.score.core.util.Parts;
import bio.overture.score.server.repository.URLGenerator;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.util.Date;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

/** Amazon specific: To generate presigned url for s3-like object storage */
public class S3URLGenerator implements URLGenerator {

  private AmazonS3 s3Client;
  private String s3Endpoint;
  private String preSignedUrlHostOverride;

  public S3URLGenerator(AmazonS3 s3Client, String s3Endpoint, String preSignedUrlHostOverride) {
    this.s3Client = s3Client;
    this.s3Endpoint = s3Endpoint;
    this.preSignedUrlHostOverride = preSignedUrlHostOverride;
  }

  public S3URLGenerator(AmazonS3 s3Client) {
    this(s3Client, null, null);
  }

  @Override
  public String getUploadPartUrl(
      String bucketName, ObjectKey objectKey, String uploadId, Part part, Date expiration) {
    GeneratePresignedUrlRequest req =
        new GeneratePresignedUrlRequest(bucketName, objectKey.getKey(), HttpMethod.PUT);
    req.setExpiration(expiration);

    req.addRequestParameter("partNumber", String.valueOf(part.getPartNumber()));
    req.addRequestParameter("uploadId", uploadId);

    val url = s3Client.generatePresignedUrl(req).toString();
    if (StringUtils.isEmpty(preSignedUrlHostOverride)) {
      return url;
    }
    return url.replace(s3Endpoint, preSignedUrlHostOverride);
  }

  @Override
  public String getDownloadPartUrl(
      String bucketName, ObjectKey objectKey, Part part, Date expiration) {
    GeneratePresignedUrlRequest req =
        new GeneratePresignedUrlRequest(bucketName, objectKey.getKey(), HttpMethod.GET);
    req.setExpiration(expiration);

    req.putCustomRequestHeader(HttpHeaders.RANGE, Parts.getHttpRangeValue(part));
    val url = s3Client.generatePresignedUrl(req).toString();
    if (StringUtils.isEmpty(preSignedUrlHostOverride)) {
      return url;
    }
    return url.replace(s3Endpoint, preSignedUrlHostOverride);
  }

  @Override
  public String getDownloadUrl(String bucketName, ObjectKey objectKey, Date expiration) {
    GeneratePresignedUrlRequest req =
        new GeneratePresignedUrlRequest(bucketName, objectKey.getKey(), HttpMethod.GET);
    req.setExpiration(expiration);
    val url = s3Client.generatePresignedUrl(req).toString();
    if (StringUtils.isEmpty(preSignedUrlHostOverride)) {
      return url;
    }
    return url.replace(s3Endpoint, preSignedUrlHostOverride);
  }
}
