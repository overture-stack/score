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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

/*
 * Using Commons Codec because Java's Integer.parseInt() wasn't behaving as expected. Codec also already a transitive dependency.
 */
public class MD5s {

  public static String toBase64(String hexMd5) throws DecoderException, UnsupportedEncodingException {
    byte[] decodedHex = Hex.decodeHex(hexMd5.toCharArray());
    // byte[] test = Integer.toString((Integer.parseInt(hexMd5))).getBytes();
    byte[] encodedHexB64 = Base64.encodeBase64(decodedHex);
    return new String(encodedHexB64, StandardCharsets.UTF_8);
  }

  public static String toHex(String base64Md5) {
    byte[] decodedHexB64 = Base64.decodeBase64(base64Md5.getBytes());
    char[] encodedHex = Hex.encodeHex(decodedHexB64);
    return String.valueOf(encodedHex);
  }

  public static boolean isBase64(String value) {
    // Azure returns MD5's padded out to 24 characters
    return (value.length() == 24) && (Base64.isBase64(value.getBytes()));
  }

  public static boolean isHex(String value) {
    return value.matches("[a-fA-F0-9]{32}");
  }

  public static boolean isEqual(String firstMD5, String secondMD5) throws UnsupportedEncodingException,
      DecoderException {
    // convert all MD5's to Base64
    String left = isHex(firstMD5) ? toBase64(firstMD5) : firstMD5;
    String right = isHex(secondMD5) ? toBase64(secondMD5) : secondMD5;
    return left.equals(right);
  }
}
