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
package org.icgc.dcc.storage.server.repository.s3;

import java.util.regex.Pattern;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.icgc.dcc.storage.server.repository.BucketNamingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

@Slf4j
@Service
@Data
public class S3BucketNamingService implements BucketNamingService {

  @Value("${bucket.name.object}")
  private String objectBucketName;
  @Value("${bucket.name.state}")
  private String stateBucketName;

  @Value("${bucket.size.pool}")
  private int bucketPoolSize;
  @Value("${bucket.size.key}")
  private int bucketKeySize;

  public final static int MAX_KEY_LENGTH = 7;
  private static Pattern P = Pattern.compile(".+\\.\\d+$");

  int doStrategyCalculation(String objectId) {
    Preconditions.checkArgument(bucketKeySize > 0);
    String piece = String.format("0x%s", scrubObjectKey(objectId).substring(0, bucketKeySize));
    return Integer.decode(piece);
  }

  int calculateIndex(String objectId) {
    int keyValue = doStrategyCalculation(objectId);
    return keyValue % bucketPoolSize;
  }

  public String getBaseObjectBucketName() {
    return objectBucketName;
  }

  public String getBaseStateBucketName() {
    return stateBucketName;
  }

  /* (non-Javadoc)
   * @see org.icgc.dcc.storage.server.repository.s3.BucketNamingService#getObjectBucketName(java.lang.String, boolean)
   */
  @Override
  public String getObjectBucketName(String objectId, boolean bypass) {
    return bypass ? objectBucketName : getObjectBucketName(objectId);
  }

  /* (non-Javadoc)
   * @see org.icgc.dcc.storage.server.repository.s3.BucketNamingService#getObjectBucketName(java.lang.String)
   */
  @Override
  public String getObjectBucketName(String objectId) {
    return getBucketName(objectId, objectBucketName);
  }

  /* (non-Javadoc)
   * @see org.icgc.dcc.storage.server.repository.s3.BucketNamingService#getStateBucketName(java.lang.String)
   */
  @Override
  public String getStateBucketName(String objectId) {
    return getBucketName(objectId, stateBucketName);
  }

  String getBucketName(String objectId, String baseName) {
    String result = baseName; // default case where bucket pool size is 0

    if (isPartitioned()) {
      int bucketIndex = calculateIndex(objectId);
      result = constructBucketName(baseName, bucketIndex);
    } else {
      log.trace("Bucket partitioning disabled: bucketPoolSize = {}", bucketKeySize, bucketPoolSize);
    }
    return result;
  }

  public String scrubObjectKey(String objectKey) {
    String[] parts = objectKey.split("/");
    if (parts.length > 1) {
      return parts[parts.length - 1];
    } else {
      return objectKey;
    }
  }

  public String constructBucketName(String baseName, int bucketIndex) {
    // deliberately does not pad single digits with leading 0's
    return String.format("%s.%d", baseName, bucketIndex);
  }

  public boolean isPartitionBucket(String bucketName) {
    if (bucketName == null) {
      return false;
    }
    return P.matcher(bucketName).matches();
  }

  public boolean isPartitioned() {
    return bucketPoolSize > 0;
  }

  public boolean validate() {
    if (StringUtils.isNotBlank(objectBucketName)) {
      throw new IllegalArgumentException("Missing Object Bucket Name configuration");
    }

    if (StringUtils.isNotBlank(stateBucketName)) {
      throw new IllegalArgumentException("Missing State Bucket Name configuration");
    }

    if (isPartitioned() && bucketKeySize <= 0) {
      throw new IllegalArgumentException("Invalid Bucket Partitioning Configuration: negative key size: "
          + bucketKeySize);
    }
    return true;
  }
}
