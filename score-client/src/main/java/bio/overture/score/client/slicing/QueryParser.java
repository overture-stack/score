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
package bio.overture.score.client.slicing;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

public class QueryParser {

  private static Splitter QUERY_SPLITTER = Splitter.on(CharMatcher.anyOf(":-")).omitEmptyStrings().trimResults();

  public static List<Slice> parse(List<String> queries) {
    val result = new ArrayList<Slice>();
    for (val region : queries) {
      result.add(parse(region));
    }

    return result;
  }

  public static Integer parseIntArgument(String s) {
    val stripped = s.replace(",", "");
    return Integer.parseInt(stripped);
  }

  public static Slice parse(String query) {
    val tokens = QUERY_SPLITTER.split(query);
    val it = tokens.iterator();
    val count = Iterators.size(tokens.iterator());

    try {
      switch (count) {
      case 1:
        return new Slice(it.next());
      case 2:
        return new Slice(it.next(), parseIntArgument(it.next()));
      case 3:
        return new Slice(it.next(), parseIntArgument(it.next()), parseIntArgument(it.next()));
      default:
        throw new IllegalArgumentException(String.format("Unrecognizable region %s", query));
      }
    } catch (NumberFormatException nfe) {
      throw new IllegalArgumentException(String.format("Invalid region specified %s", query), nfe);
    }
  }

}
