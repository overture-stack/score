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
package org.icgc.dcc.storage.test.auth;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_LENGTH;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.DataOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import org.icgc.dcc.common.core.security.SSLCertificateValidation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.BaseEncoding;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor
public class AuthClient {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @NonNull
  private final String serverUrl;

  @SneakyThrows
  public String createAccessToken() {
    SSLCertificateValidation.disable();

    val url = new URL(
        serverUrl + "/oauth/token");
    val connection = (HttpsURLConnection) url.openConnection();
    val userCredentials = "mgmt:pass";
    val basicAuth =
        "Basic " + new String(BaseEncoding.base64().encode(userCredentials.getBytes(StandardCharsets.UTF_8)));
    byte[] data =
        "grant_type=password&username=workflow&scope=aws.upload%20aws.download".getBytes(StandardCharsets.UTF_8);
    connection.setDoOutput(true);
    connection.setUseCaches(false);
    connection.setRequestMethod("POST");
    connection.setRequestProperty("charset", UTF_8.name());
    connection.setRequestProperty(AUTHORIZATION, basicAuth);
    connection.setRequestProperty(ACCEPT, "application/json");
    connection.setRequestProperty(CONTENT_TYPE, "application/x-www-form-urlencoded");
    connection.setRequestProperty(CONTENT_LENGTH, Integer.toString(data.length));

    try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
      wr.write(data);
    }

    return MAPPER.readValue(connection.getInputStream(), ObjectNode.class).get("access_token").textValue();
  }

}
