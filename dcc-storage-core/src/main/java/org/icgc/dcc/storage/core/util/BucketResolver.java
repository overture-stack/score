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
package org.icgc.dcc.storage.core.util;

import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BucketResolver {

  public static int MAX_KEY_LENGTH = 7;
  private static Pattern P = Pattern.compile(".+\\.\\d+$");

  public static int doStrategyCalculation(String objectId, int keyLength) {
    String piece = String.format("0x%s", scrubObjectKey(objectId).substring(0, keyLength));
    return Integer.decode(piece);
  }

  public static int calculateIndex(String objectId, int bucketPoolSize, int keyLength) {
    int keyValue = doStrategyCalculation(objectId, keyLength);
    return keyValue % bucketPoolSize;
  }

  public static String getBucketName(String objectId, String baseName, int bucketPoolSize, int keyLength) {
    String result = baseName; // default case where bucket pool size is 0

    if ((keyLength < 0) || (keyLength > MAX_KEY_LENGTH)) {
      throw new IllegalArgumentException(String.format(
          "Bucket Partitioning key length of %d exceeds maximum allowable key length of %d", keyLength, MAX_KEY_LENGTH));
    }

    if ((keyLength > 0) && (bucketPoolSize > 0)) {
      int bucketIndex = calculateIndex(objectId, bucketPoolSize, keyLength);
      result = constructBucketName(baseName, bucketIndex);
    } else {
      log.trace("Bucket partitioning disabled: keyLength = {} and bucketPoolSize = {}", keyLength, bucketPoolSize);
    }
    return result;
  }

  public static String scrubObjectKey(String objectKey) {
    String[] parts = objectKey.split("/");
    if (parts.length > 1) {
      return parts[parts.length - 1];
    } else {
      return objectKey;
    }
  }

  /*
   * deliberately does not pad single digits with leading 0's
   */
  public static String constructBucketName(String baseName, int bucketIndex) {
    return String.format("%s.%d", baseName, bucketIndex);
  }

  public static boolean isBucketPartitioned(String bucketName) {
    if (bucketName == null) {
      return false;
    }
    return P.matcher(bucketName).matches();
  }
}
