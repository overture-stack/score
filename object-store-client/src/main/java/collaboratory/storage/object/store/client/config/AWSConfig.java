/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package collaboratory.storage.object.store.client.config;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * AWS configuration.
 */
@Lazy
@Configuration
public class AWSConfig {

  static {
    HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
    {

      @Override
      public boolean verify(String hostname, SSLSession session)
      {
        return true;
      }
    });
  }

  @Autowired
  private ClientProperties properties;

  @Bean
  public AmazonS3 s3() {
    AmazonS3 s3Client = null;
    if (properties.getAwsAccessKey() != null && properties.getAwsSecretKey() != null) {
      s3Client = new AmazonS3Client(new BasicAWSCredentials(
          properties.getAwsAccessKey(), properties.getAwsSecretKey()),
          clientConfiguration());
    } else {
      s3Client = new AmazonS3Client(new ProfileCredentialsProvider(), clientConfiguration());

    }

    if (properties.getKeyStoneEndPoint() != null) {
      System.out.println("OS Endpoint: " + properties.getKeyStoneEndPoint());
      s3Client.setEndpoint(properties.getKeyStoneEndPoint());
    }

    return s3Client;
  }

  @Bean
  public AmazonEC2 ec2() {
    AmazonEC2 ec2Client = null;
    if (properties.getAwsAccessKey() != null && properties.getAwsSecretKey() != null) {
      ec2Client = new AmazonEC2Client(new BasicAWSCredentials(
          properties.getAwsAccessKey(), properties.getAwsSecretKey()),
          clientConfiguration());
    } else {
      ec2Client = new AmazonEC2Client(new ProfileCredentialsProvider(), clientConfiguration());

    }
    return ec2Client;
  }

  private ClientConfiguration clientConfiguration() {
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setProtocol(Protocol.HTTPS);
    clientConfiguration
        .setRetryPolicy(PredefinedRetryPolicies
            .getDefaultRetryPolicyWithCustomMaxRetries(properties
                .getRetryLimit()));
    return clientConfiguration;

  }
}
