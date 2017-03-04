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
package org.icgc.dcc.storage.server.repository;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.core.model.Part;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * a simple way to divide an object into multi parts
 */
@Slf4j
public class SimplePartCalculator implements PartCalculator {

  private static final int MAX_NUM_PART = 10000;
  private static final int MIN_PART_SIZE = 20 * 1024 * 1024; // 20MB

  private final int minPartSize;

  public SimplePartCalculator(int minPartSize) {
    this.minPartSize = Math.max(minPartSize, MIN_PART_SIZE);
  }

  @Override
  public List<Part> divide(long fileSize) {
    return divide(0, fileSize);
  }

  @Override
  public List<Part> divide(long offset, long objectLength) {
    int defaultPartSize = Math.max(minPartSize, (int) (objectLength / MAX_NUM_PART) + 1);
    log.debug("Part Size: {}", defaultPartSize);
    Builder<Part> parts = ImmutableList.builder();
    long currentTotalLength = 0;
    for (int i = 1; currentTotalLength < objectLength; ++i) {
      int partSize = (int) Math.min(defaultPartSize, objectLength - currentTotalLength);
      parts.add(new Part(i, partSize, offset + currentTotalLength, null, null, null));
      currentTotalLength += partSize;
    }
    return parts.build();
  }

  @Override
  public List<Part> specify(long offset, long length) {
    return ImmutableList.of(new Part(1, length, offset, null, null, null));
  }
}
