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
package org.icgc.dcc.storage.server.repository;

import static org.junit.Assert.assertEquals;

import org.icgc.dcc.storage.server.repository.s3.S3BucketNamingService;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class BucketNamingServiceTest {

  public static final String OBJECT_BASE_BUCKET = "oicr.icgc";
  public static final String STATE_BASE_BUCKET = "oicr.icgc.state";

  S3BucketNamingService sut;

  @Before
  public void setUp() {
    // Configure
    sut = new S3BucketNamingService();
    sut.setObjectBucketName(OBJECT_BASE_BUCKET);
    sut.setStateBucketName(STATE_BASE_BUCKET);
  }

  @Test(expected = IllegalArgumentException.class)
  public void key_length_0() {
    sut.setBucketPoolSize(10);
    sut.setBucketKeySize(0);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194");
    assertEquals(OBJECT_BASE_BUCKET, value);
  }

  @Test
  public void key_length_0_and_pool_size_0() {
    sut.setBucketPoolSize(0);
    sut.setBucketKeySize(0);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194");
    assertEquals(OBJECT_BASE_BUCKET, value);
  }

  @Test
  public void pool_size_0() {
    sut.setBucketPoolSize(0);
    sut.setBucketKeySize(3);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194");
    assertEquals(OBJECT_BASE_BUCKET, value);
  }

  @Test
  public void object_id_key_value_same_as_max_pool_size() {
    // test based on value of object id
    sut.setBucketPoolSize(16);
    sut.setBucketKeySize(1);
    String value = sut.getObjectBucketName("f6f3cb04-38b4-574c-bffb-af1426113194");
    String expected = String.format("%s.15", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void object_id_key_value_0() {
    // test based on value of object id
    sut.setBucketPoolSize(16);
    sut.setBucketKeySize(1);
    String value = sut.getObjectBucketName("06f3cb04-38b4-574c-bffb-af1426113194");
    String expected = String.format("%s.0", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_1_pool_smaller() {
    sut.setBucketPoolSize(2);
    sut.setBucketKeySize(1);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194");
    String expected = String.format("%s.1", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_1_pool_larger() {
    sut.setBucketPoolSize(20);
    sut.setBucketKeySize(1);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194");
    String expected = String.format("%s.5", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_2_pool_smaller() {
    sut.setBucketPoolSize(10);
    sut.setBucketKeySize(2);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194");
    String expected = String.format("%s.6", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_2_pool_larger() {
    sut.setBucketPoolSize(100);
    sut.setBucketKeySize(2);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194");
    String expected = String.format("%s.86", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_2_pool_larger_no_padding() {
    sut.setBucketPoolSize(100);
    sut.setBucketKeySize(2);
    String value = sut.getObjectBucketName("06f3cb04-38b4-574c-bffb-af1426113194");
    String expected = String.format("%s.6", OBJECT_BASE_BUCKET); // not zero-padded i.e. "%s.06"
    assertEquals(expected, value);
  }

  @Test
  public void key_length_3_pool_smaller() {
    sut.setBucketPoolSize(10);
    sut.setBucketKeySize(3);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194"); // 1391
    String expected = String.format("%s.1", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_3_pool_larger() {
    sut.setBucketPoolSize(2000);
    sut.setBucketKeySize(3);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194"); // 1391
    String expected = String.format("%s.1391", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_max_pool_smaller() {
    sut.setBucketPoolSize(10);
    sut.setBucketKeySize(S3BucketNamingService.MAX_KEY_LENGTH);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194"); // 91176112
    String expected = String.format("%s.2", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test
  public void key_length_max_pool_larger() {
    sut.setBucketPoolSize(99999999);
    sut.setBucketKeySize(S3BucketNamingService.MAX_KEY_LENGTH);
    String value = sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194"); // 91176112
    String expected = String.format("%s.91176112", OBJECT_BASE_BUCKET);
    assertEquals(expected, value);
  }

  @Test(expected = IllegalArgumentException.class)
  public void key_length_too_large() {
    sut.setBucketPoolSize(20);
    sut.setBucketKeySize(10);
    sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194");
  }

  @Test(expected = IllegalArgumentException.class)
  public void key_length_negative() {
    sut.setBucketPoolSize(20);
    sut.setBucketKeySize(-3);
    sut.getObjectBucketName("56f3cb04-38b4-574c-bffb-af1426113194");
  }

  @Test(expected = NumberFormatException.class)
  public void invalid_object_id() {
    sut.setBucketPoolSize(10);
    sut.setBucketKeySize(3);
    sut.getObjectBucketName("Lorem ipsum dolor");
  }

  @Test
  public void key_with_data_path() {
    String key = "data/56f3cb04-38b4-574c-bffb-af1426113194";
    String objectId = sut.scrubObjectKey(key);
    assertEquals(objectId, "56f3cb04-38b4-574c-bffb-af1426113194");
  }

  @Test
  public void key_with_random_path() {
    String key = "/booga/booga/56f3cb04-38b4-574c-bffb-af1426113194";
    String objectId = sut.scrubObjectKey(key);
    assertEquals(objectId, "56f3cb04-38b4-574c-bffb-af1426113194");
  }

  @Test
  public void key_without_path() {
    String key = "56f3cb04-38b4-574c-bffb-af1426113194";
    String objectId = sut.scrubObjectKey(key);
    assertEquals(objectId, "56f3cb04-38b4-574c-bffb-af1426113194");
  }

  public static void main(String[] arg) {
    S3BucketNamingService sut = new S3BucketNamingService();
    sut.setObjectBucketName(OBJECT_BASE_BUCKET);
    sut.setStateBucketName(STATE_BASE_BUCKET);
    sut.setBucketKeySize(3);
    sut.setBucketPoolSize(32);

    System.out.println(sut.getObjectBucketName("a82efa12-9aac-558b-9f51-beb21b7a2298"));

  }
}
