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
package org.icgc.dcc.storage.client.mount;

import static com.google.common.collect.Iterables.get;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import lombok.NoArgsConstructor;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public class MountOptions {

  /**
   * Constants.
   */
  private static final Splitter OPTIONS_SPLITTER = Splitter.on(",")
      .omitEmptyStrings()
      .trimResults();
  private static final Splitter OPTION_SPLITTER = Splitter.on("=")
      .omitEmptyStrings()
      .trimResults();

  public static Map<String, String> parseOptions(String value) {
    val options = Maps.<String, String> newLinkedHashMap();
    val list = splitOptions(value);
    for (val element : list) {
      val option = splitOption(element);

      val k = get(option, 0);
      val v = get(option, 1, null);
      options.put(k, v);
    }

    return options;
  }

  public static String formatOptions(Map<String, String> options) {
    return options.entrySet().stream().map(formatOption()).collect(joining(","));
  }

  private static Function<? super Entry<String, String>, ? extends String> formatOption() {
    return entry -> entry.getKey() + (entry.getValue() == null ? "" : "=" + entry.getValue());
  }

  private static List<String> splitOptions(String value) {
    return OPTIONS_SPLITTER.splitToList(value);
  }

  private static List<String> splitOption(final java.lang.String element) {
    return OPTION_SPLITTER.splitToList(element);
  }

}
