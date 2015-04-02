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
package collaboratory.storage.object.store.client.upload;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import collaboratory.storage.object.store.core.model.CompletedPart;
import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.store.core.util.ChannelUtils;

import com.google.common.collect.Sets;

@Slf4j
@Component
public class ObjectUpload {

  @Autowired
  private ObjectUploadServiceProxy proxy;

  public void upload(File file, String objectId, boolean redo) throws IOException {
    if (redo) {
      // create another version of the object
      startUpload(file, objectId, true);
    } else {
      try {
        resume(file, objectId);
      } catch (Exception e) {
        // TODO: only start upload if it is a not found exception
        // check if object exists, exit
        startUpload(file, objectId, false);
      }

    }
  }

  private void startUpload(File file, String objectId, boolean overwrite) throws IOException {
    UploadSpecification spec = proxy.initiateUpload(objectId, file.length());
    uploadParts(spec.getParts(), file, objectId, spec.getUploadId());
  }

  private void resume(File file, String objectId) throws IOException {
    UploadProgress progress = proxy.getProgress(objectId);
    final Set<Integer> completedPartNumber = Sets.newHashSet();

    for (CompletedPart part : progress.getCompletedParts()) {
      completedPartNumber.add(part.getPartNumber());
    }

    val parts = progress.getParts();
    parts.removeIf(new Predicate<Part>() {

      @Override
      public boolean test(Part part) {
        return completedPartNumber.contains(part.getPartNumber());
      }
    });
    uploadParts(parts, file, progress.getObjectId(), progress.getUploadId());

    // TODO: run checksum on the completed part on a separate thread
  }

  private void uploadParts(List<Part> parts, File file, String objectId, String uploadId) throws IOException {
    for (Part part : parts) {
      String etag = ChannelUtils.UploadObject(file, new URL(part.getUrl()), part.getOffset(), part.getPartSize());
      proxy.finalizeUploadPart(objectId, uploadId, part.getPartNumber(), etag, etag);
    }
    proxy.finalizeUpload(objectId, uploadId);
  }

}
