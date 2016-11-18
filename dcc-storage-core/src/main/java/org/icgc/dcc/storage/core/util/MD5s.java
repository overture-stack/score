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
package org.icgc.dcc.storage.core.util;

import com.google.common.io.BaseEncoding;

/*
 * Using Commons Codec because Java's Integer.parseInt() wasn't behaving as expected. Codec also already a transitive dependency.
 */
public class MD5s {

  public static String toBase64(String hexMd5) {
    byte[] decodedHex = BaseEncoding.base16().lowerCase().decode(hexMd5);
    return BaseEncoding.base64().encode(decodedHex);
  }

  public static String toHex(String base64Md5) {
    byte[] decodedHexB64 = BaseEncoding.base64().decode(base64Md5);
    return BaseEncoding.base16().lowerCase().encode(decodedHexB64);
  }

  public static boolean isBase64(String value) {
    // Azure returns MD5's padded out to 24 characters
    try {
      BaseEncoding.base64().decode(value);
      return (value.length() == 24);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static boolean isHex(String value) {
    try {
      BaseEncoding.base16().lowerCase().decode(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static boolean isEqual(String firstMD5, String secondMD5) {
    // convert all MD5's to Base64
    String left = isHex(firstMD5) ? toBase64(firstMD5) : firstMD5;
    String right = isHex(secondMD5) ? toBase64(secondMD5) : secondMD5;
    return left.equalsIgnoreCase(right);
  }
}
