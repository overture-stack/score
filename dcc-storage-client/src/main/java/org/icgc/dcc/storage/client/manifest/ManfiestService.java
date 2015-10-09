/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.storage.client.manifest;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.io.Resources;

import lombok.SneakyThrows;
import lombok.val;

@Service
public class ManfiestService {

  /**
   * Constants
   */
  private static final ManifestReader READER = new ManifestReader();

  /**
   * Configuration
   */
  @Value("${portal.url}")
  private String portalUrl;

  @SneakyThrows
  public String getManifestContent(ManifestResource resource) {
    val url = resolveManifestUrl(resource);
    return Resources.toString(url, UTF_8);
  }

  @SneakyThrows
  public Manifest getManifest(ManifestResource resource) {
    val url = resolveManifestUrl(resource);
    return getManifestByURL(url);
  }

  @SneakyThrows
  public Manifest getManifestById(String manifestId) {
    val url = resolvePortalManifestUrl(manifestId);
    return READER.readManifest(url);
  }

  @SneakyThrows
  public Manifest getManifestByURL(URL manifestUrl) {
    return READER.readManifest(manifestUrl);
  }

  @SneakyThrows
  public Manifest getManifestByFile(File manifestFile) {
    return READER.readManifest(manifestFile);
  }

  @SneakyThrows
  private URL resolveManifestUrl(ManifestResource resource) {
    if (resource.getType() == ManifestResource.Type.URL) {
      return new URL(resource.getValue());
    } else if (resource.getType() == ManifestResource.Type.ID) {
      return resolvePortalManifestUrl(resource.getValue());
    } else {
      return new File(resource.getValue()).toURI().toURL();
    }
  }

  private URL resolvePortalManifestUrl(String manifestId) throws MalformedURLException {
    return new URL(String.format("%s/api/v1/repository/manifests/%s", portalUrl, manifestId));
  }

}
