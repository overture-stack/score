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
package org.icgc.dcc.storage.core.model;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class ObjectSpecification {

  private String objectKey;
  private String objectId;
  private String uploadId;
  private List<Part> parts;
  private long objectSize;
  private String objectMd5;

  // Flag indicating whether the meta data was found in the expected bucket, or
  // in the "fallback" bucket (created prior to bucket partitioning)
  private boolean isRelocated = false;

  public ObjectSpecification() {
    super();
  }

  public ObjectSpecification(String objectKey, String objectId, String uploadId, List<Part> parts, long objectSize,
      boolean isRelocated) {
    super();
    this.objectKey = objectKey;
    this.objectId = objectId;
    this.uploadId = uploadId;
    this.parts = parts;
    this.objectSize = objectSize;
    this.isRelocated = isRelocated;
  }

  /**
   * @return the objectKey
   */
  public String getObjectKey() {
    return objectKey;
  }

  /**
   * @param objectKey the objectKey to set
   */
  public void setObjectKey(String objectKey) {
    this.objectKey = objectKey;
  }

  /**
   * @return the objectId
   */
  public String getObjectId() {
    return objectId;
  }

  /**
   * @param objectId the objectId to set
   */
  public void setObjectId(String objectId) {
    this.objectId = objectId;
  }

  /**
   * @return the uploadId
   */
  public String getUploadId() {
    return uploadId;
  }

  /**
   * @param uploadId the uploadId to set
   */
  public void setUploadId(String uploadId) {
    this.uploadId = uploadId;
  }

  /**
   * @return the parts
   */
  public List<Part> getParts() {
    return parts;
  }

  /**
   * @param parts the parts to set
   */
  public void setParts(List<Part> parts) {
    this.parts = parts;
  }

  /**
   * @return the objectSize
   */
  public long getObjectSize() {
    return objectSize;
  }

  /**
   * @param objectSize the objectSize to set
   */
  public void setObjectSize(long objectSize) {
    this.objectSize = objectSize;
  }

  /**
   * @return the objectMd5
   */
  @JsonIgnore
  public String getObjectMd5() {
    return objectMd5;
  }

  /**
   * @param objectMd5 the objectMd5 to set
   */
  @JsonIgnore
  public void setObjectMd5(String objectMd5) {
    this.objectMd5 = objectMd5;
  }

  /**
   * @return the isRelocated
   */
  public boolean isRelocated() {
    return isRelocated;
  }

  /**
   * @param isRelocated the isRelocated to set
   */
  public void setRelocated(boolean isRelocated) {
    this.isRelocated = isRelocated;
  }

  public boolean hasPartChecksums() {
    int presentCount = 0;
    if (parts != null) {
      for (Part p : parts) {
        if (p.getSourceMd5() != null) {
          presentCount += 1;
        }
        if (presentCount < parts.size()) {
          log.warn("Some parts missing MD5 checksum (but other parts have one)");
        }
      }
    }
    return (presentCount > 0);
  }
}
