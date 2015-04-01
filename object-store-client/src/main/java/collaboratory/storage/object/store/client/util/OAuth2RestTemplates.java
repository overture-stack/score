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
package collaboratory.storage.object.store.client.util;

import static lombok.AccessLevel.PRIVATE;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;

import collaboratory.storage.object.store.core.util.DumbHostnameVerifier;
import collaboratory.storage.object.store.core.util.DumbX509TrustManager;

@NoArgsConstructor(access = PRIVATE)
public final class OAuth2RestTemplates {

  @SneakyThrows
  public static void disableCertificateChecks(OAuth2RestTemplate oauthTemplate) {
    HttpsURLConnection.setDefaultHostnameVerifier(new DumbHostnameVerifier());

    val sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] { new DumbX509TrustManager() }, null);

    val requestFactory = new SSLContextRequestFactory(sslContext);
    oauthTemplate.setRequestFactory(requestFactory);

    val provider = new ResourceOwnerPasswordAccessTokenProvider();
    provider.setRequestFactory(requestFactory);

    oauthTemplate.setAccessTokenProvider(provider);
  }

}
