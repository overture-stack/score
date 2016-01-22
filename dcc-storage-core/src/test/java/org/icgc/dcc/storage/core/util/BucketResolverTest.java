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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * 
 */
public class BucketResolverTest {

  public static final String BASE_BUCKET = "oicr.icgc";

  @Test
  public void key_length_0() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 10, 0);
    assertEquals(BASE_BUCKET, value);
  }

  @Test
  public void key_length_0_and_pool_size_0() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 0, 0);
    assertEquals(BASE_BUCKET, value);
  }

  @Test
  public void pool_size_0() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 0, 3);
    assertEquals(BASE_BUCKET, value);
  }

  @Test
  public void key_value_same_as_max_pool_size() {
    String value = BucketResolver.getBucketName("f6f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 16, 1);
    String expected = String.format("%s.15", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_value_0() {
    String value = BucketResolver.getBucketName("06f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 16, 1);
    String expected = String.format("%s.0", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_1_pool_smaller() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 2, 1);
    String expected = String.format("%s.1", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_1_pool_larger() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 20, 1);
    String expected = String.format("%s.5", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_2_pool_smaller() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 10, 2);
    String expected = String.format("%s.6", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_2_pool_larger() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 100, 2);
    String expected = String.format("%s.86", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_2_pool_larger_no_padding() {
    String value = BucketResolver.getBucketName("06f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 100, 2);
    String expected = String.format("%s.6", BASE_BUCKET); // not zero-padded i.e. "%s.06"
    assertEquals(expected, value);
  }

  @Test
  public void key_length_3_pool_smaller() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 10, 3); // 1391
    String expected = String.format("%s.1", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_3_pool_larger() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 2000, 3); // 1391
    String expected = String.format("%s.1391", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_max_pool_smaller() {
    String value =
        BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 10,
            BucketResolver.MAX_KEY_LENGTH); // 91176112
    String expected = String.format("%s.2", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_max_pool_larger() {
    String value =
        BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 99999999,
            BucketResolver.MAX_KEY_LENGTH); // 91176112
    String expected = String.format("%s.91176112", BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test(expected = IllegalArgumentException.class)
  public void key_length_too_large() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 20, 10);
  }

  @Test(expected = IllegalArgumentException.class)
  public void key_length_negative() {
    String value = BucketResolver.getBucketName("56f3cb04-38b4-574c-bffb-af1426113194", BASE_BUCKET, 20, -3);
  }

  @Test(expected = NumberFormatException.class)
  public void invalid_object_id() {
    String value = BucketResolver.getBucketName("Lorem ipsum dolor", BASE_BUCKET, 10, 3);
  }

  @Test
  public void key_with_data_path() {
    String key = "data/56f3cb04-38b4-574c-bffb-af1426113194";
    String objectId = BucketResolver.scrubObjectKey(key);
    assertEquals(objectId, "56f3cb04-38b4-574c-bffb-af1426113194");
  }

  @Test
  public void key_with_random_path() {
    String key = "/booga/booga/56f3cb04-38b4-574c-bffb-af1426113194";
    String objectId = BucketResolver.scrubObjectKey(key);
    assertEquals(objectId, "56f3cb04-38b4-574c-bffb-af1426113194");
  }

  @Test
  public void key_without_path() {
    String key = "56f3cb04-38b4-574c-bffb-af1426113194";
    String objectId = BucketResolver.scrubObjectKey(key);
    assertEquals(objectId, "56f3cb04-38b4-574c-bffb-af1426113194");
  }
}
