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
package collaboratory.storage.object.store.client.slicing;

import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMSequenceDictionary;

import java.util.Comparator;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SliceConverter {

  @NonNull
  private final SAMSequenceDictionary samDictionary;

  public QueryInterval convert(Slice slice) {
    int ix = samDictionary.getSequenceIndex(slice.getSequence());
    QueryInterval interval = new QueryInterval(ix, slice.getStart(), slice.getEnd());
    return interval;
  }

  public QueryInterval[] convert(List<Slice> slices) {
    QueryInterval[] result = new QueryInterval[slices.size()];
    sortSlices(slices);
    int index = 0;
    for (Slice s : slices) {
      result[index] = convert(s);
      index++;
    }
    return result;
  }

  public void sortSlices(List<Slice> slices) {
    Comparator<Slice> groupByComparator = new AlphanumComparator().thenComparing(Slice::getStart);
    slices.sort(groupByComparator);
  }
}
