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

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import lombok.SneakyThrows;
import lombok.val;

/**
 * Configurations for SSL
 */
@Lazy
@Configuration
public class SSLClientConfig {

  @Autowired
  private ClientProperties properties;

  @SneakyThrows
  @Bean
  public KeyStore trustStore() {
    val ssl = properties.getSsl();
    if (ssl.getTrustName() != null && ssl.getTrustStore() != null) {
      InputStream is = ssl.getTrustStore().getInputStream();
      KeyStore trustStore = KeyStore.getInstance(ssl.getTrustStoreType());
      trustStore.load(is, ssl.getTrustStorePassword().toCharArray());
      return trustStore;
    } else {
      return null;
    }
  }

  @Bean
  public X509HostnameVerifier verifier() {
    return new AbstractVerifier() {

      @Override
      public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
        for (String cn : cns) {
          if (cn.equals(properties.getSsl().getTrustName())) {
            return;
          }
        }

        verify(host, cns, subjectAlts, true);
      }

    };
  }

  @Bean
  @SneakyThrows
  public KeyManagerFactory keyManagerFactory() {
    return null;
  }

  @Bean
  @SneakyThrows
  public TrustManagerFactory trustManagerFactory() {
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    if (properties.getSsl().getTrustStore() == null) {
      tmf.init((KeyStore) null);
      return tmf;
    }

    tmf.init(trustStore());
    return tmf;
  }

}
