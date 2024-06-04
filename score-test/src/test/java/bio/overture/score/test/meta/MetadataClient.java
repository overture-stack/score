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
package bio.overture.score.test.meta;

import static bio.overture.score.core.util.Collectors.toImmutableList;

import bio.overture.score.server.security.ssl.SSLCertificateValidation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Responsible for interacting with metadata service. */
@Service
public class MetadataClient {

  /** Constants. */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** Configuration. */
  @NonNull @Getter private final String serverUrl;

  @Autowired
  public MetadataClient(
      @Value("${metadata.url}") String serverUrl, @Value("${metadata.ssl.enabled}") boolean ssl) {
    if (!ssl) {
      SSLCertificateValidation.disable();
    }

    this.serverUrl = serverUrl;
  }

  public Entity findEntity(@NonNull String objectId) {
    return read("/" + objectId);
  }

  public List<Entity> findEntities() {
    return readAll("/");
  }

  public List<Entity> findEntitiesByGnosId(@NonNull String gnosId) {
    return readAll("?gnosId=" + gnosId);
  }

  @SneakyThrows
  private Entity read(@NonNull String path) {
    return MAPPER.readValue(resolveUrl(path), Entity.class);
  }

  @SneakyThrows
  private List<Entity> readAll(@NonNull String path) {
    val results = Lists.<Entity>newArrayList();
    boolean last = false;
    int pageNumber = 0;

    while (!last) {
      val url =
          resolveUrl(path + (path.contains("?") ? "&" : "?") + "size=2000&page=" + pageNumber);
      val result = MAPPER.readValue(url, ObjectNode.class);
      last = result.path("last").asBoolean();
      List<Entity> page =
          MAPPER.convertValue(result.path("content"), new TypeReference<ArrayList<Entity>>() {});

      results.addAll(page);
      pageNumber++;
    }

    // Remove potential duplicates due to inserts on paging:
    // See https://jira.oicr.on.ca/browse/COL-491
    return results.stream().distinct().collect(toImmutableList());
  }

  @SneakyThrows
  private URL resolveUrl(String path) {
    return new URL(serverUrl + "/entities" + path);
  }
}
