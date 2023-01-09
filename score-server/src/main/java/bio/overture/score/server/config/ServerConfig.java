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
package bio.overture.score.server.config;

import bio.overture.score.core.util.SimplePartCalculator;
import bio.overture.score.server.repository.URLGenerator;
import bio.overture.score.server.repository.BucketNamingService;
import bio.overture.score.core.util.PartCalculator;
import bio.overture.score.server.repository.UploadStateStore;
import bio.overture.score.server.repository.s3.S3BucketNamingService;
import bio.overture.score.server.repository.s3.S3URLGenerator;
import bio.overture.score.server.repository.s3.S3UploadStateStore;
import com.amazonaws.services.s3.AmazonS3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Server level configuration
 */
@Configuration
@Profile({ "aws", "collaboratory", "default" })
public class ServerConfig {

  @Value("${upload.partsize}")
  private int partSize;

  @Value("${s3.preSignedUrl.override.host}")
  private String preSignedUrlsHostOverride;

  @Value("${s3.endpoint}")
  private String s3Endpoint;

  @Bean
  public UploadStateStore stateStore() {
    return new S3UploadStateStore();
  }

  @Bean
  public PartCalculator calculator() {
    return new SimplePartCalculator(partSize);
  }

  @Bean
  public URLGenerator url(AmazonS3 s3Client) {
    return new S3URLGenerator(s3Client, s3Endpoint, preSignedUrlsHostOverride);
  }

  @Bean
  public BucketNamingService bucketNamingService() {
    return new S3BucketNamingService();
  }
}
