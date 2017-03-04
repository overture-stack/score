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
package org.icgc.dcc.storage.server;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.storage.server.config.S3Config;
import org.icgc.dcc.storage.server.repository.UploadService;
import org.icgc.dcc.storage.server.repository.s3.S3BucketNamingService;
import org.icgc.dcc.storage.server.repository.s3.S3UploadService;
import org.icgc.dcc.storage.server.repository.s3.S3UploadStateStore;

@NoArgsConstructor(access = PRIVATE)
public class Tests {

  public static final String DATA_DIR = "data";
  public static final String UPLOAD_DIR = "upload";
  public static final String OBJECT_BUCKET_NAME = "oicr.icgc";
  public static final String STATE_BUCKET_NAME = "oicr.icgc.state";

  public static UploadService createUploadService() {
    val endpoint = "https://www.cancercollaboratory.org:9080";
    val s3Config = new S3Config();
    s3Config.setEndpoint(endpoint);
    val s3Client = s3Config.s3();

    val namingService = new S3BucketNamingService();
    namingService.setObjectBucketName(OBJECT_BUCKET_NAME);
    namingService.setStateBucketName(STATE_BUCKET_NAME);
    val stateStore = new S3UploadStateStore();
    stateStore.setBucketNamingService(namingService);
    stateStore.setUploadDir(UPLOAD_DIR);
    stateStore.setS3Client(s3Client);

    val uploadService = new S3UploadService();
    stateStore.setBucketNamingService(namingService);
    uploadService.setDataDir(DATA_DIR);
    uploadService.setS3Conf(s3Config);
    uploadService.setS3Client(s3Client);
    uploadService.setStateStore(stateStore);

    return uploadService;
  }

}
