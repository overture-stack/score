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

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.openstack.swift.v1.SwiftApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * AWS configuration.
 */
@Lazy
@Configuration
public class SwiftConfig {

  @Autowired
  private ClientProperties properties;

  @Bean
  public SwiftApi swift() {
    return ContextBuilder.newBuilder("openstack-swift")
        .endpoint(properties.getKeyStoneEndPoint())
        .credentials(properties.getAwsAccessKey(), properties.getAwsSecretKey())
        .buildApi(SwiftApi.class);
  }

  @Bean
  public BlobStoreContext blobstore() {
    String provider = "swift-keystone";
    BlobStoreContext context = ContextBuilder.newBuilder(provider)
        .endpoint(properties.getKeyStoneEndPoint())
        .credentials(properties.getAwsAccessKey(), properties.getAwsSecretKey())
        .buildView(BlobStoreContext.class);

    return context;

  }
}
