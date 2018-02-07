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
import htsjdk.samtools.SAMFileHeader;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QueryHandler {

  public static List<Slice> parseQueryStrings(@NonNull List<String> query) {
    // Handle if multiple ranges specified
    return QueryParser.parse(query);
  }

  public static QueryInterval[] convertSlices(@NonNull SAMFileHeader header, @NonNull List<Slice> slices) {
    val converter = new SliceConverter(header.getSequenceDictionary());
    val intervals = converter.convert(slices);

    // remove nulls - happens when the query specifies sequences that don't exist in SQ
    List<QueryInterval> list = new ArrayList<QueryInterval>(Arrays.asList(intervals));
    list.removeAll(Collections.singleton(null));
    val cleaned = list.toArray(new QueryInterval[list.size()]);

    return QueryInterval.optimizeIntervals(cleaned); // otherwise triggers an assertion
  }
}
