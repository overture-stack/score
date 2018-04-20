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
package bio.overture.score.client.cli;

import java.util.Arrays;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import lombok.val;

public abstract class EnumConverter<T extends Enum<T>> extends BaseConverter<T> {

  private final Class<T> enumClass;

  public EnumConverter(Class<T> enumClass, String optionName) {
    super(optionName);
    this.enumClass = enumClass;
  }

  @Override
  public T convert(String value) {
    try {
      val normalize = normalizeValue(value);
      return Enum.valueOf(enumClass, normalize);
    } catch (Exception e) {
      val values = formatValues();
      throw new ParameterException(getErrorString(value, "a value in " + values));
    }
  }

  private String normalizeValue(String value) {
    return value.toUpperCase().replaceAll("-", "_");
  }

  private String formatValues() {
    return Arrays.toString(enumClass.getEnumConstants()).toLowerCase().replaceAll("_", "-");
  }

}