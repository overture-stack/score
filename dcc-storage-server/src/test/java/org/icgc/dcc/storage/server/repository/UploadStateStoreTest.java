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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import lombok.val;

import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.server.repository.s3.S3BucketNamingService;
import org.icgc.dcc.storage.server.repository.s3.S3UploadStateStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

@RunWith(MockitoJUnitRunner.class)
public class UploadStateStoreTest {

  /**
   * Constants.
   */
  private static final String OBJECT_BUCKET_NAME = "oicr.icgc";
  private static final String STATE_BUCKET_NAME = "oicr.icgc";

  /**
   * Dependencies.
   */
  @Mock
  AmazonS3 s3Client;

  /**
   * Subject.
   */
  @InjectMocks
  S3UploadStateStore store;

  @Before
  public void setUp() {
    // Configure
    S3BucketNamingService namingService = new S3BucketNamingService();
    namingService.setObjectBucketName(OBJECT_BUCKET_NAME);
    namingService.setStateBucketName(STATE_BUCKET_NAME);
    namingService.setBucketPoolSize(0);
    store.setBucketNamingService(namingService);

    // store.setStateBucketName(BUCKET_NAME);
    store.setUploadDir("upload");
  }

  @Test
  public void testCreate() throws Exception {
    val objectId = "objectId1";
    val uploadId = "uploadId1";
    val spec = new ObjectSpecification();
    spec.setObjectId(objectId);
    spec.setUploadId(uploadId);
    spec.setObjectKey("objectKey1");
    spec.setObjectSize(1000);
    spec.setParts(null);

    store.create(spec);

    verify(s3Client).putObject(
        eq(OBJECT_BUCKET_NAME),
        eq("upload/" + objectId + "_" + uploadId + "/.meta"),
        any(InputStream.class),
        any(ObjectMetadata.class));
  }

  @Test
  public void testRead() throws Exception {
    val objectId = "objectId1";
    val uploadId = "uploadId1";
    val json = "{\"objectId\":\"" + objectId + "\",\"uploadId\":\"" + uploadId + "\"}";

    val s3Object = mock(S3Object.class);
    val inputStream = new S3ObjectInputStream(new ByteArrayInputStream(json.getBytes(UTF_8)), null);
    when(s3Object.getObjectContent()).thenReturn(inputStream);
    when(s3Client.getObject(any())).thenReturn(s3Object);

    val spec = store.read(objectId, uploadId);

    assertThat(spec.getObjectId()).isEqualTo(objectId);
    assertThat(spec.getUploadId()).isEqualTo(uploadId);
  }

  @Test
  public void testFormatUploadPartName() throws Exception {
    val partNumber = 17;
    val json = "{\"x\":1}";
    val partName = S3UploadStateStore.formatUploadPartName(partNumber, json);
    assertThat(partName).isEqualTo("part-00000011|{\"x\":1}");
  }

}
