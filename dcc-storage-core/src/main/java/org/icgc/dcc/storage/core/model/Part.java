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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * An entity to represent a part that the client will be uploaded
 */
@JsonIgnoreProperties(ignoreUnknown = true)
final public class Part implements Comparable<Part> {

  int partNumber;
  long partSize;
  long offset;
  String url;
  String md5; // md5 of a local copy of the part (i.e., after a download)
  String sourceMd5; // original md5 of part; set when uploaded to S3/Ceph

  public Part() {
    super();
  }

  /**
   * @param partNumber
   * @param partSize
   * @param offset
   * @param url
   * @param md5
   */
  public Part(int partNumber, long partSize, long offset, String url, String md5) {
    super();
    this.partNumber = partNumber;
    this.partSize = partSize;
    this.offset = offset;
    this.url = url;
    this.md5 = md5;
  }

  /**
   * @return the partNumber
   */
  public int getPartNumber() {
    return partNumber;
  }

  /**
   * @param partNumber the partNumber to set
   */
  public void setPartNumber(int partNumber) {
    this.partNumber = partNumber;
  }

  /**
   * @return the partSize
   */
  public long getPartSize() {
    return partSize;
  }

  /**
   * @param partSize the partSize to set
   */
  public void setPartSize(long partSize) {
    this.partSize = partSize;
  }

  /**
   * @return the offset
   */
  public long getOffset() {
    return offset;
  }

  /**
   * @param offset the offset to set
   */
  public void setOffset(long offset) {
    this.offset = offset;
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @return the md5
   */
  public String getMd5() {
    return md5;
  }

  /**
   * @param md5 the md5 to set
   */
  public void setMd5(String md5) {
    this.md5 = md5;
  }

  /**
   * @return the sourceMd5
   */
  @JsonIgnore
  public String getSourceMd5() {
    return sourceMd5;
  }

  /**
   * @param sourceMd5 the sourceMd5 to set
   */
  @JsonIgnore
  public void setSourceMd5(String sourceMd5) {
    this.sourceMd5 = sourceMd5;
  }

  @JsonIgnore
  public boolean isCompleted() {
    return md5 != null;
  }

  @Override
  public int compareTo(Part otherPart) {
    return this.partNumber - otherPart.partNumber;
  }

  @JsonIgnore
  public boolean hasFailedChecksum() {
    return !sourceMd5.equals(md5);
  }

  @JsonIgnore
  public boolean isMissingSourceMd5() {
    return (sourceMd5 == null) || sourceMd5.trim().isEmpty();
  }
}
