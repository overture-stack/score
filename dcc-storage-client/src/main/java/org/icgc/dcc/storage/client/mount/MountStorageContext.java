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
package org.icgc.dcc.storage.client.mount;

import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Multimaps.index;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.storage.fs.StorageFile.storageFile;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.storage.client.download.DownloadService;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.core.model.ObjectInfo;
import org.icgc.dcc.storage.fs.StorageContext;
import org.icgc.dcc.storage.fs.StorageFile;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor
public class MountStorageContext implements StorageContext {

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
  @Getter(lazy = true, value = PRIVATE)
  private final Map<String, StorageFile> fileIdIndex = resolveFileIndex();
  @Getter(lazy = true, value = PRIVATE)
  private final Multimap<String, StorageFile> fileGnosIdIndex = resolveFileGnosIdIndex();
  @Getter(lazy = true, value = PRIVATE)
  private final LoadingCache<String, URL> urlCache = createURLCache();
  @Getter(lazy = true)
  private final boolean authorized = resolveAuthorized();

  public boolean resolveAuthorized() {
    try {
      val probe = objects.get(0);
      val probeUrl = downloadService.getUrl(probe.getId());
      probeUrl.openStream();
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  @Override
  @SneakyThrows
  public URL getUrl(String objectId) {
    return getUrlCache().get(objectId);
  }

  @Override
  public StorageFile getFileById(String id) {
    return getFileIdIndex().get(id);
  }

  @Override
  public Collection<StorageFile> getFilesByGnosId(String gnosId) {
    return getFileGnosIdIndex().get(gnosId);
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

  private Map<String, StorageFile> resolveFileIndex() {
    return uniqueIndex(getFiles(), StorageFile::getId);
  }

  private Multimap<String, StorageFile> resolveFileGnosIdIndex() {
    return index(getFiles(), StorageFile::getGnosId);
  }

  private LoadingCache<String, URL> createURLCache() {
    return CacheBuilder.newBuilder().build(CacheLoader.from(objectId -> downloadService.getUrl(objectId)));
  }

}
