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
package org.icgc.dcc.score.client.config;

import java.security.KeyStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.ssl.SSLContexts;
import org.icgc.dcc.score.client.ssl.TrustedCnHostnameVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.SneakyThrows;
import lombok.val;

/**
 * Configurations for SSL
 */
@Configuration
public class SSLClientConfig {

  @Autowired
  ClientProperties properties;

  @Bean
  @SneakyThrows
  public TrustManagerFactory trustManagerFactory() {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

    val ssl = properties.getSsl();
    if (ssl.isCustom()) {
      factory.init(trustStore());
    } else {
      factory.init((KeyStore) null);
    }

    return factory;
  }

  @Bean
  @SneakyThrows
  public KeyStore trustStore() {
    val ssl = properties.getSsl();
    if (ssl.isCustom()) {
      val is = ssl.getTrustStore().getInputStream();
      val trustStore = KeyStore.getInstance(ssl.getTrustStoreType());
      trustStore.load(is, ssl.getTrustStorePassword().toCharArray());

      return trustStore;
    } else {
      return null;
    }
  }

  @Bean
  @SneakyThrows
  public SSLContext sslContext() {
    val ssl = properties.getSsl();
    if (ssl.isCustom()) {
      return SSLContexts.custom().loadTrustMaterial(trustStore(), null).build();
    } else {
      return SSLContexts.createDefault();
    }
  }

  @Bean
  public HostnameVerifier hostnameVerifier() {
    val ssl = properties.getSsl();
    if (ssl.isCustom()) {
      return new TrustedCnHostnameVerifier(ssl.getTrustName());
    } else {
      return new DefaultHostnameVerifier();
    }
  }

}
