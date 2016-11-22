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

import java.util.List;

import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.server.repository.PartCalculator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

@Slf4j
public class AzurePartCalculator implements PartCalculator {

  // Note: Azure has a MAX_NUM_PART = 50000; constraint, but that only applies to uploads
  // which we are currently delegating entirely to the Azure Java SDK.
  private static final int MAX_PART_SIZE = 4 * 1024 * 1024; // 4 MB

  private int partSize = MAX_PART_SIZE;

  public AzurePartCalculator() {
  }

  public AzurePartCalculator(int maxPartSize) {
    this.partSize = maxPartSize;
  }

  public void setPartSize(int size) {
    partSize = size;
  }

  @Override
  public List<Part> divide(long fileSize) {
    return divide(0, fileSize);
  }

  @Override
  public List<Part> divide(long offset, long objectLength) {
    log.debug("Part Size: {}", partSize);
    Builder<Part> parts = ImmutableList.builder();
    long currentTotalLength = 0;
    for (int i = 1; currentTotalLength < objectLength; ++i) {
      val size = (int) Math.min(partSize, objectLength - currentTotalLength);
      parts.add(new Part(i, size, offset + currentTotalLength, null, null, null));
      currentTotalLength += size;
    }
    return parts.build();
  }

  @Override
  public List<Part> specify(long offset, long length) {
    return ImmutableList.of(new Part(1, length, offset, null, null, null));
  }

}
