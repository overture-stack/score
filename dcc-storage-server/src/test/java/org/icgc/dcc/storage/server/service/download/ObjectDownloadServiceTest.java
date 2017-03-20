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
package org.icgc.dcc.storage.server.service.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.regex.Pattern;

import org.icgc.dcc.storage.core.util.ObjectKeys;
import org.icgc.dcc.storage.server.config.ServerConfig;
import org.icgc.dcc.storage.server.exception.IdNotFoundException;
import org.icgc.dcc.storage.server.repository.SimplePartCalculator;
import org.icgc.dcc.storage.server.repository.s3.S3BucketNamingService;
import org.icgc.dcc.storage.server.repository.s3.S3DownloadService;
import org.icgc.dcc.storage.server.repository.s3.S3URLGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Splitter;

import lombok.val;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration(classes = ServerConfig.class)
public class ObjectDownloadServiceTest extends S3DownloadService {

  private String endpointUrl = "https://storage.icgctest.org";
  private String objectBucketName = "oicr.icgc";
  private String stateBucketName = "oicr.icgc.state";
  private String dataDir = "data";
  private String objectId = "a82efa12-9aac-558b-9f51-beb21b7a2298"; // length 1 : 10; length2 : 168; length 3 : 2690

  /**
   * Dependencies.
   */
  @Mock
  AmazonS3 s3Client;

  /**
   * SUT
   */
  @InjectMocks
  S3DownloadService service;

  S3BucketNamingService namingService = new S3BucketNamingService();

  @Before
  public void setUp() {
    namingService.setObjectBucketName(objectBucketName);
    namingService.setStateBucketName(stateBucketName);
    namingService.setBucketPoolSize(16);
    namingService.setBucketKeySize(3);
    service.setBucketNamingService(namingService);

    // ReflectionTestUtils.setField(service, "dataBucketName", dataBucketName);
    // ReflectionTestUtils.setField(service, "stateBucketName", stateBucketName);
    ReflectionTestUtils.setField(service, "dataDir", dataDir);
    // ReflectionTestUtils.setField(service, "bucketPoolSize", 5);
    // ReflectionTestUtils.setField(service, "bucketKeySize", 2);
    ReflectionTestUtils.setField(service, "expiration", 7);

    ReflectionTestUtils.setField(service, "urlGenerator", new S3URLGenerator());
    ReflectionTestUtils.setField(service, "partCalculator", new SimplePartCalculator(20000));
  }

  @Test(expected = IdNotFoundException.class)
  public void it_takes_two_to_fail_with_not_found() {
    // stubbing appears before the actual execution
    val firstException = new AmazonServiceException("Didn't find Object Id in bucket");
    firstException.setStatusCode(HttpStatus.NOT_FOUND.value());
    when(s3Client.getObject(Mockito.any())).thenThrow(firstException, firstException); // stubs first two calls to
                                                                                       // s3Client.getObject()
    service.download(objectId, 0, 1000, false);
  }

  @Test
  public void verify_fallback_in_download_presigned_urls() throws Exception {
    // stubbing appears before the actual execution
    val firstException = new AmazonServiceException("Didn't find Object Id in bucket");
    firstException.setStatusCode(HttpStatus.NOT_FOUND.value());

    int bucketPoolSize = 32;
    int bucketKeySize = 2;

    namingService.setBucketPoolSize(bucketPoolSize);
    namingService.setBucketKeySize(bucketKeySize);

    // Have to stub out half the universe:
    val urlGen = new S3URLGenerator();
    ReflectionTestUtils.setField(urlGen, "s3Client",
        ObjectDownloadServiceStubFactory.createS3ClientForRadosGW(endpointUrl));
    ReflectionTestUtils.setField(service, "urlGenerator", urlGen);

    when(s3Client.getObject(Mockito.any())).thenThrow(firstException);

    val parts = ObjectDownloadServiceStubFactory.createParts(5); // based on 104857600 size / 20971520 part size
    val os = ObjectDownloadServiceStubFactory.createObjectSpecification(objectId,
        ObjectKeys.getObjectKey(dataDir, objectId), 104857600);
    os.setParts(parts);

    os.setRelocated(true); // this is the main test input

    // need to stub out one of the methods on the System Under Test itself
    val sut = spy(service);
    doReturn(os).when(sut).getSpecification(objectId);

    val objSpec = sut.download(objectId, 0, 104857600, false);

    val p = objSpec.getParts().get(0);

    val url = new URL(p.getUrl());
    String path = url.getPath();
    if (path.startsWith("/")) {
      path = path.substring(1, path.length() - 1);
    }
    val bucket = Splitter.on('/').trimResults().omitEmptyStrings().split(path).iterator().next();
    assertEquals(objectBucketName, bucket);

    System.out.println();
  }

  @Test
  public void verify_partitioned_buckets_in_download_presigned_urls() throws Exception {
    // stubbing appears before the actual execution
    val firstException = new AmazonServiceException("Didn't find Object Id in bucket");
    firstException.setStatusCode(HttpStatus.NOT_FOUND.value());

    int bucketPoolSize = 32;
    int bucketKeySize = 2;

    namingService.setBucketPoolSize(bucketPoolSize);
    namingService.setBucketKeySize(bucketKeySize);

    // Have to stub out half the universe:
    val urlGen = new S3URLGenerator();
    ReflectionTestUtils.setField(urlGen, "s3Client",
        ObjectDownloadServiceStubFactory.createS3ClientForRadosGW(endpointUrl));
    ReflectionTestUtils.setField(service, "urlGenerator", urlGen);

    when(s3Client.getObject(Mockito.any())).thenThrow(firstException);

    val parts = ObjectDownloadServiceStubFactory.createParts(5); // based on 104857600 size / 20971520 part size
    val os = ObjectDownloadServiceStubFactory.createObjectSpecification(objectId,
        ObjectKeys.getObjectKey(dataDir, objectId), 104857600);
    os.setParts(parts);

    os.setRelocated(false); // this is the main test input

    // need to stub out one of the methods on the System Under Test itself
    val sut = spy(service);
    doReturn(os).when(sut).getSpecification(objectId);

    val objSpec = sut.download(objectId, 0, 104857600, false);

    val p = objSpec.getParts().get(0);

    val url = new URL(p.getUrl());
    String path = url.getPath();
    if (path.startsWith("/")) {
      path = path.substring(1, path.length() - 1);
    }
    val bucket = Splitter.on('/').trimResults().omitEmptyStrings().split(path).iterator().next();
    assertTrue((objectBucketName.length() < bucket.length()) && (bucket.startsWith(objectBucketName)));

    // assert that bucket name ends in a .number
    val pattern = Pattern.compile(".+\\.\\d+$");
    assertTrue(pattern.matcher(bucket).matches());

    System.out.println();
  }
}
