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
package org.icgc.dcc.storage.server.repository.azure;

import static org.icgc.dcc.storage.core.util.UUIDs.isUUID;

import lombok.Setter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

import org.icgc.dcc.storage.core.model.ObjectInfo;
import org.icgc.dcc.storage.server.repository.ListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;

@Slf4j
@Setter
@Service
@Profile("azure")
public class AzureListingService implements ListingService {

  @Autowired
  private CloudBlobContainer container;

  @Override
  @Cacheable("listing")
  public List<ObjectInfo> getListing() {
    log.info(String.format("Mounting to '%s' (%s)", container.getName(), container.getUri().toString()));
    val listing = Lists.<ObjectInfo> newArrayList();

    // read from fallback bucket - any files from prior to bucket partitioning
    for (ListBlobItem blobItem : container.listBlobs()) {
      try {
        val blob = (CloudBlob) blobItem;
        val objInfo = createInfo(blob);
        if (objInfo.getId() != null) {
          listing.add(objInfo);
        }
      } catch (ClassCastException e) {
        log.error("Encountered a non-CloudBlob item in container: {} ({})", blobItem.getUri().toString(), blobItem
            .getClass().getName());
      }
    }

    return listing;
  }

  private ObjectInfo createInfo(CloudBlob blob) {
    return new ObjectInfo(getObjectId(blob.getName()), blob.getProperties().getLastModified().getTime(), blob
        .getProperties()
        .getLength());
  }

  private static String getObjectId(String fname) {
    val name = new File(fname).getName();

    // Only UUIDs correspond to published objects
    // (this filters out sentinel objects like 'heliograph')
    return isUUID(name) ? name : null;
  }
}
