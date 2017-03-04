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
package org.icgc.dcc.storage.server.config;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Data
@Slf4j
@Profile("azure")
@ConfigurationProperties(prefix = "azure")
public class AzureConfig {

  private String endpointProtocol;
  private String accountName;
  private String accountKey;

  @Value("${bucket.name.object}")
  private String containerName;

  public String storageConnectionString() {
    return String.format("DefaultEndpointsProtocol=%s;AccountName=%s;AccountKey=%s", endpointProtocol, accountName,
        accountKey);
  }

  private CloudBlobClient azureClient() throws InvalidKeyException, URISyntaxException {
    CloudStorageAccount account = CloudStorageAccount.parse(storageConnectionString());
    // Create a blob service client
    return account.createCloudBlobClient();
  }

  @Bean
  public CloudBlobContainer azureContainer() throws URISyntaxException, StorageException, InvalidKeyException {
    CloudBlobContainer result = azureClient().getContainerReference(containerName);
    if (!result.exists()) {
      log.error(String.format("What the? No '%s' container found", containerName));
      throw new IllegalStateException(String.format("Container '%s' not found for Azure Blob Storage account '%s'",
          containerName, accountName));
    }
    return result;
  }

}
