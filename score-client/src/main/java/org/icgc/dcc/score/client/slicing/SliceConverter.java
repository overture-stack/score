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
package org.icgc.dcc.score.client.slicing;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMSequenceDictionary;

import java.util.List;
import java.util.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.score.client.util.AlphanumComparator;

@RequiredArgsConstructor
public class SliceConverter {

  /**
   * Dependencies.
   */
  @NonNull
  private final SAMSequenceDictionary samDictionary;

  public Optional<QueryInterval> convert(@NonNull Slice slice) {
    val index = samDictionary.getSequenceIndex(slice.getSequence());
    if (index == -1) {
      return Optional.empty();
    }
    return Optional.ofNullable(new QueryInterval(index, slice.getStart(), slice.getEnd()));
  }

  public QueryInterval[] convert(@NonNull List<Slice> slices) {
    sortSlices(slices);

    int index = 0;
    val result = new QueryInterval[slices.size()];
    for (val slice : slices) {
      val mapping = convert(slice);
      if (mapping.isPresent()) {
        result[index] = mapping.get();
        index++;
      }
    }
    return result;
  }

  public void sortSlices(List<Slice> slices) {
    val groupByComparator = new AlphanumComparator().thenComparing(Slice::getStart);
    slices.sort(groupByComparator);
  }

}
