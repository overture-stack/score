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

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

@RequiredArgsConstructor
public class SSLContextRequestFactory extends SimpleClientHttpRequestFactory {

  @NonNull
  private final SSLContext sslContext;

  @Override
  protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
    if (connection instanceof HttpsURLConnection) {
      ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
    }

    super.prepareConnection(connection, httpMethod);
  }

}