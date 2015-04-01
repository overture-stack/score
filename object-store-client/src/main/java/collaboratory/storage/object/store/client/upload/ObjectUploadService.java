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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import collaboratory.storage.object.store.core.model.CompletedPart;
import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.store.core.util.ChannelUtils;

import com.google.common.collect.Sets;

@Slf4j
@Service
public class ObjectUploadService {

  @Value("${collaboratory.upload.endpoint}")
  private String endpoint;

  @Autowired
  @Qualifier("upload")
  private RestTemplate req;

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
    UploadSpecification spec = initiateUpload(objectId, file.length());
    uploadParts(spec.getParts(), file, objectId, spec.getUploadId());
  }

  private void resume(File file, String objectId) throws IOException {
    HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
    UploadProgress progress = req.exchange(endpoint + "/upload/{object-id}", HttpMethod.GET,
        requestEntity,
        UploadProgress.class, objectId).getBody();
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
      finalizeUploadPart(objectId, uploadId, part.getPartNumber(), etag, etag);
    }
    finalizeUpload(objectId, uploadId);
  }

  private String getToken() {
    return "token";

  }

  private UploadSpecification initiateUpload(String objectId, long length) {
    HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
    return req.exchange(endpoint + "/upload/{object-id}/uploads?fileSize={file-size}", HttpMethod.POST, requestEntity,
        UploadSpecification.class, objectId, length).getBody();
  }

  private void finalizeUpload(String objectId, String uploadId) {

    HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());
    req.exchange(endpoint + "/upload/{object-id}?uploadId={upload-id}", HttpMethod.POST, requestEntity,
        Void.class, objectId, uploadId);
  }

  private void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String etag) {
    HttpEntity<Object> requestEntity = new HttpEntity<Object>(defaultHeaders());

    req.exchange(
        endpoint + "/upload/{object-id}/parts?uploadId={upload-id}&partNumber={partNumber}&md5={md5}&etag={etag}",
        HttpMethod.POST, requestEntity,
        Void.class, objectId, uploadId, partNumber, md5, etag);
  }

  private HttpHeaders defaultHeaders() {
    HttpHeaders requestHeaders = new HttpHeaders();
    requestHeaders.set("access-token", getToken());
    return requestHeaders;
  }
}
