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

import java.util.List;

import lombok.val;

import org.icgc.dcc.storage.core.model.ObjectKey;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.server.repository.s3.S3FetchedObject;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.SignerFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.internal.S3Signer;
import com.google.common.collect.ImmutableList;

public class ObjectDownloadServiceStubFactory {

  public static List<Part> createParts(int numParts) {
    ImmutableList.Builder<Part> builder = ImmutableList.builder();
    for (int i = 0; i < numParts; i++) {
      val p = new Part();
      p.setMd5("md5");
      p.setPartNumber(i + 1);
      p.setPartSize(1000);
      builder.add(p);
    }
    return builder.build();
  }

  public static ObjectSpecification createObjectSpecification(String objectId, ObjectKey objectKey, int objectSize) {
    val result = new ObjectSpecification();
    result.setObjectId(objectId);
    result.setObjectKey(objectKey.getKey());
    result.setObjectSize(objectSize);
    return result;
  }

  public static S3FetchedObject createS3Object() {
    val result = new S3FetchedObject(null);
    result.setRelocated(true);
    return result;
  }

  public static AmazonS3 createS3ClientForRadosGW(String endpoint) {
    // Configs copied from S3Config
    // RadosGW doesn't support latest AWS API version: AWSS3V4SignerType
    val clientConfiguration = new ClientConfiguration();
    SignerFactory.registerSigner("S3Signer", S3Signer.class);
    clientConfiguration.setSignerOverride("S3SignerType");
    clientConfiguration.setProtocol(Protocol.HTTPS);
    val s3Client = new AmazonS3Client(new BasicAWSCredentials("accesskey", "secret"), clientConfiguration);
    s3Client.setEndpoint(endpoint);
    s3Client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true));
    return s3Client;
  }
}
