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
package bio.overture.score.client.manifest.icgc;

import static java.nio.charset.StandardCharsets.UTF_8;

import bio.overture.score.client.manifest.DownloadManifest;
import bio.overture.score.client.manifest.ManifestResource;
import bio.overture.score.client.manifest.ManifestService;
import bio.overture.score.client.manifest.UploadManifest;
import com.google.common.io.Resources;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!kf")
// Fixme No usage found for this class
public class IcgcManifestService implements ManifestService {

  /** Constants */
  private static final DownloadManifestReader DOWNLOAD_READER = new DownloadManifestReader();

  private static final UploadManifestReader UPLOAD_READER = new UploadManifestReader();

  /** Configuration */
  @Value("${portal.url}")
  private String portalUrl;

  public static interface MService {
    String getManifestContent(ManifestResource resource);

    DownloadManifest getDownloadManifest(ManifestResource resource);

    UploadManifest getUploadManifest(ManifestResource resource);
  }

  @Override
  @SneakyThrows
  public String getManifestContent(ManifestResource resource) {
    val url = resolveManifestUrl(resource);
    return Resources.toString(url, UTF_8);
  }

  @Override
  @SneakyThrows
  public DownloadManifest getDownloadManifest(ManifestResource resource) {
    val url = resolveManifestUrl(resource);
    return getDownloadManifestByURL(url);
  }

  @SneakyThrows
  public DownloadManifest getDownloadManifestById(String manifestId) {
    val url = resolvePortalManifestUrl(manifestId);
    return readDownloadManifestByURL(url);
  }

  @SneakyThrows
  public DownloadManifest getDownloadManifestByURL(URL manifestUrl) {
    return readDownloadManifestByURL(manifestUrl);
  }

  @SneakyThrows
  public DownloadManifest getDownloadManifestByFile(File manifestFile) {
    return DOWNLOAD_READER.readManifest(manifestFile);
  }

  private DownloadManifest readDownloadManifestByURL(URL url) {
    try {
      return DOWNLOAD_READER.readManifest(url);
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not read manifest from '" + url + "': " + e.getMessage(), e);
    }
  }

  @Override
  @SneakyThrows
  public UploadManifest getUploadManifest(ManifestResource resource) {
    val url = resolveManifestUrl(resource);
    return getUploadManifestByURL(url);
  }

  @SneakyThrows
  public UploadManifest getUploadManifestById(String manifestId) {
    val url = resolvePortalManifestUrl(manifestId);
    return readUploadManifestByURL(url);
  }

  @SneakyThrows
  public UploadManifest getUploadManifestByURL(URL manifestUrl) {
    return readUploadManifestByURL(manifestUrl);
  }

  @SneakyThrows
  public UploadManifest getUploadManifestByFile(File manifestFile) {
    return UPLOAD_READER.readManifest(manifestFile);
  }

  private UploadManifest readUploadManifestByURL(URL url) {
    try {
      return UPLOAD_READER.readManifest(url);
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not read manifest from '" + url + "': " + e.getMessage(), e);
    }
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
    return new URL(String.format("%s/api/v1/manifests/%s", portalUrl, manifestId));
  }
}
