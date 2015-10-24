/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.storage.client.cli;

import static com.google.common.collect.Iterables.get;

import java.util.List;
import java.util.Map;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import lombok.val;

public class MountOptionsConverter extends BaseConverter<Map<String, String>> {

  /**
   * Constants.
   */
  private static final Splitter OPTIONS_SPLITTER = Splitter.on(",")
      .omitEmptyStrings()
      .trimResults();
  private static final Splitter OPTION_SPLITTER = Splitter.on("=")
      .omitEmptyStrings()
      .trimResults();

  public MountOptionsConverter(String optionName) {
    super(optionName);
  }

  @Override
  public Map<String, String> convert(String value) {
    try {
      val options = Maps.<String, String> newHashMap();
      val list = splitOptions(value);
      for (val element : list) {
        val option = splitOption(element);

        val k = get(option, 0);
        val v = get(option, 1, null);
        options.put(k, v);
      }

      return options;
    } catch (Exception ex) {
      throw new ParameterException(getErrorString(value, "mount options"));
    }
  }

  private static List<String> splitOptions(String value) {
    return OPTIONS_SPLITTER.splitToList(value);
  }

  private static List<String> splitOption(final java.lang.String element) {
    return OPTION_SPLITTER.splitToList(element);
  }

}
