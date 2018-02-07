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
package org.icgc.dcc.score.core.util;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.icgc.dcc.score.core.model.ObjectKey;

/**
 * Object key related utilities.
 */
@NoArgsConstructor(access = PRIVATE)
public final class ObjectKeys {

  /**
   * Returns S3 key for actual object blob
   */
  public static ObjectKey getObjectKey(@NonNull String dataDir, @NonNull String objectId) {
    return new ObjectKey(dataDir, objectId);
  }

  public static String getObjectId(@NonNull String dataDir, @NonNull String objectKey) {
    return objectKey.replaceAll(dataDir + "/", "");
  }

  /**
   * Returns S3 key for metadata file for blob (contains upload id's, MD5 checksums, pre-signed URL's for each part of
   * file)
   */
  public static String getObjectMetaKey(@NonNull String dataDir, @NonNull String objectId) {
    return dataDir + "/" + objectId + ".meta";
  }

}
