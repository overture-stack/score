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
package bio.overture.score.client.metadata;

import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Profile("!gen3")
public class LegacyMetadataService implements MetadataService {

  @Autowired
  private LegacyMetadataClient legacyMetadataClient;

  public List<Entity> getEntities(String... fields) {
    return legacyMetadataClient.findEntities(fields);
  }

  public Entity getEntity(String objectId) {
    return legacyMetadataClient.findEntity(objectId);
  }

  public Optional<Entity> getIndexEntity(Entity entity) {
    val entities = legacyMetadataClient.findEntitiesByGnosId(entity.getGnosId());
    return entities
        .stream()
        .filter(e -> isIndexFile(e, entity.getFileName()))
        .findFirst();
  }

  private static boolean isIndexFile(Entity e, String fileName) {
    return isBaiFile(e, fileName) || isTbiFile(e, fileName) || isIdxFile(e, fileName);
  }

  private static boolean isTbiFile(Entity e, String fileName) {
    return isMatch(e, fileName + ".tbi");
  }

  private static boolean isIdxFile(Entity e, String fileName) {
    return isMatch(e, fileName + ".idx");
  }

  private static boolean isBaiFile(Entity e, String fileName) {
    return isMatch(e, fileName + ".bai");
  }

  private static boolean isMatch(Entity e, String indexFileName) {
    return e.getFileName().compareToIgnoreCase(indexFileName) == 0;
  }

}
