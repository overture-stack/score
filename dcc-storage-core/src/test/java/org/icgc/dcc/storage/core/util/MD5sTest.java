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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MD5sTest {

  public static final String MD5_HEX_1 = "2bdf4f6127a6971262c023a492e6fb27";
  public static final String MD5_BASE64_1 = "K99PYSemlxJiwCOkkub7Jw==";

  public static final String MD5_HEX_2 = "cebdd6af9b12933da261f0464acfd808";
  public static final String MD5_BASE64_2 = "zr3Wr5sSkz2iYfBGSs/YCA==";

  public static final String GARBAGE = "data%2F040c742c-b160-5661";

  @Test
  public void is_hex() {
    assertThat(MD5s.isHex(MD5_HEX_1)).isTrue();
  }

  @Test
  public void is_not_hex() {
    assertThat(MD5s.isHex(MD5_BASE64_1)).isFalse();
  }

  @Test
  public void is_base64() {
    assertThat(MD5s.isBase64(MD5_BASE64_2)).isTrue();
  }

  @Test
  public void is_not_base64() {
    assertThat(MD5s.isBase64(MD5_HEX_2)).isFalse();
  }

  @Test
  public void test_to_base64() {
    assertThat(MD5s.toBase64(MD5_HEX_1)).isEqualToIgnoringCase(MD5_BASE64_1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_to_base64_non_hex() {
    // assertj 3 has some nice syntactic sugar for this:
    // assertThatExceptionOfType(DecoderException.class).isThrownBy(() -> { MD5s.toBase64(GARBAGE); });
    MD5s.toBase64(GARBAGE);
  }

  @Test
  public void test_to_hex() {
    assertThat(MD5s.toHex(MD5_BASE64_2)).isEqualToIgnoringCase(MD5_HEX_2);
  }

  @Test
  public void test_to_hex_non_base64() {
    assertThat(MD5s.toHex(MD5_BASE64_2)).isEqualToIgnoringCase(MD5_HEX_2);
  }

  @Test
  public void test_is_equal() {
    assertThat(MD5s.isEqual(MD5_HEX_1, MD5_BASE64_1)).isTrue();
    assertThat(MD5s.isEqual(MD5_BASE64_2, MD5_HEX_2)).isTrue();
  }

}
