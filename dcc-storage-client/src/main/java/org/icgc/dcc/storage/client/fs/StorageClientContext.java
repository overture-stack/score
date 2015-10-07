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
package org.icgc.dcc.storage.client.fs;

import static com.google.common.collect.Maps.uniqueIndex;
import static org.icgc.dcc.storage.fs.StorageFile.storageFile;

import java.net.URL;
import java.util.List;

import org.icgc.dcc.storage.client.download.DownloadService;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.core.model.ObjectInfo;
import org.icgc.dcc.storage.fs.StorageContext;
import org.icgc.dcc.storage.fs.StorageFile;

import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class StorageClientContext implements StorageContext {

  /**
   * Dependencies
   */
  @NonNull
  private final DownloadService downloadService;

  /**
   * Caches.
   */
  private final List<Entity> entities;
  private final List<ObjectInfo> objects;

  @Getter(lazy = true)
  private final List<StorageFile> files = resolveFiles();

  @Override
  public URL getUrl(String objectId) {
    return downloadService.getUrl(objectId);
  }

  private List<StorageFile> resolveFiles() {
    val entityIndex = uniqueIndex(entities, Entity::getId);

    val files = ImmutableList.<StorageFile> builder();
    for (val object : objects) {
      val id = object.getId();
      val entity = entityIndex.get(id);
      if (entity == null) {
        continue;
      }

      // Join entity to object
      files.add(
          storageFile()
              .id(id)
              .fileName(entity.getFileName())
              .gnosId(entity.getGnosId())
              .lastModified(object.getLastModified())
              .size(object.getSize())
              .build());
    }

    return files.build();
  }

}
