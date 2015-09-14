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
package collaboratory.storage.object.store.client.slicing;

import static com.google.common.base.Preconditions.checkState;

import java.util.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import collaboratory.storage.object.store.client.transport.MetaServiceProxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
@Component
public class MetaServiceQuery {

  @Autowired
  private MetaServiceProxy metaService;

  private ObjectNode objectNode;

  private String objectFileName;

  public void setObjectId(String objectId) {
    objectNode = metaService.findEntity(objectId);
    objectFileName = objectNode.get("fileName").textValue();
    if (!objectFileName.toLowerCase().endsWith(".bam")) {
      throw new IllegalArgumentException("Cannot view non-BAM files");
    }
  }

  public String getFileName() {
    checkState(objectFileName != null, "Object Id not specified");
    return objectFileName;
  }

  /**
   * Uses GNOS id associated with current BAM file object id
   * @param entity
   * @return
   */
  public Optional<String> getAssociatedIndexObjectId() {
    val gnosId = objectNode.get("gnosId").textValue();
    ObjectNode entities = metaService.findEntitiesByGnosId(gnosId);
    Optional<String> indexFileObjectId = findIndexFileObjectId(entities);
    if (!indexFileObjectId.isPresent()) {
      log.error("Cannot find object id of index file");
      return Optional.empty();
    }
    return indexFileObjectId;
  }

  private Optional<String> findIndexFileObjectId(final ObjectNode entities) {
    // loop through all entities associated with GNOS id
    for (JsonNode entity : entities.withArray("content")) {
      val fileName = entity.get("fileName").textValue();
      if (fileName.endsWith(".bai")) {
        return Optional.of(entity.get("id").textValue());
      }
    }
    return Optional.empty();
  }
}
