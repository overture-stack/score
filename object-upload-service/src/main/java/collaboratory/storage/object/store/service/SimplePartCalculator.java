/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package collaboratory.storage.object.store.service;

import java.util.List;

import collaboratory.storage.object.store.core.model.Part;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class SimplePartCalculator implements ObjectPartCalculator {

  private static final int MAX_NUM_PART = 10000;
  // minimum size for S3
  private static final int PART_SIZE = 5242880;

  @Override
  public List<Part> divide(long fileSize) {
    long defaultPartSize = Math.max(PART_SIZE, fileSize / MAX_NUM_PART + 1);
    long filePosition = 0;
    Builder<Part> parts = ImmutableList.builder();
    for (int i = 1; filePosition < fileSize; ++i) {
      long partSize = Math.min(defaultPartSize, fileSize - filePosition);
      parts.add(new Part(i, partSize, filePosition, null));
      filePosition += partSize;
    }
    return parts.build();
  }
}
