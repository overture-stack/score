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
package collaboratory.storage.object.store.controller;

import java.io.IOException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import collaboratory.storage.object.store.core.model.ObjectSpecification;
import collaboratory.storage.object.store.core.model.UploadProgress;
import collaboratory.storage.object.store.service.upload.ObjectUploadService;
import collaboratory.storage.object.store.util.TokenHasher;

/**
 * A controller to expose RESTful API for upload
 */
@Setter
@RestController
@RequestMapping("/upload")
@Slf4j
@Profile({ "prod", "default", "debug" })
public class ObjectUploadController {

  @Autowired
  ObjectUploadService uploadService;

  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}/uploads")
  public @ResponseBody ObjectSpecification initializeMultipartUpload(
      @RequestHeader(value = "Authorization", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "overwrite", required = false, defaultValue = "false") boolean overwrite,
      @RequestParam(value = "fileSize", required = true) long fileSize) {
    log.info("Initiating upload of object id {} with access token {} (MD5) having size of {}", objectId,
        TokenHasher.hashToken(accessToken),
        Long.toString(fileSize));
    return uploadService.initiateUpload(objectId, fileSize, overwrite);
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "/{object-id}/parts")
  @ResponseStatus(value = HttpStatus.OK)
  public void deletePart(
      @RequestHeader(value = "Authorization", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "partNumber", required = true) int partNumber,
      @RequestParam(value = "uploadId", required = true) String uploadId) {
    uploadService.deletePart(objectId, uploadId, partNumber);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}/parts")
  @ResponseStatus(value = HttpStatus.OK)
  public void finalizePartUpload(
      @RequestHeader(value = "Authorization", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "partNumber", required = true) int partNumber,
      @RequestParam(value = "uploadId", required = true) String uploadId,
      @RequestParam(value = "md5", required = true) String md5,
      @RequestParam(value = "etag", required = true) String eTag) {
    uploadService.finalizeUploadPart(objectId, uploadId, partNumber, md5, eTag);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void finalizeUpload(
      @RequestHeader(value = "Authorization", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "uploadId", required = true) String uploadId
      ) {
    uploadService.finalizeUpload(objectId, uploadId);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}/recovery")
  @ResponseStatus(value = HttpStatus.OK)
  public void tryRecover(
      @RequestHeader(value = "Authorization", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "fileSize", required = true) long fileSize
      ) {
    uploadService.recover(objectId, fileSize);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/{object-id}/status")
  public @ResponseBody UploadProgress getUploadProgress(
      @RequestHeader(value = "Authorization", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "fileSize", required = true) long fileSize
      ) {
    // TODO: if object id/upload id does not exist, throw not found exception
    return uploadService.getUploadStatus(objectId, uploadService.getUploadId(objectId), fileSize);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/{object-id}")
  public @ResponseBody Boolean isObjectExist(@RequestHeader("Authorization") final String accessToken,
      @PathVariable("object-id") String objectId) {
    return uploadService.exists(objectId);
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "/{object-id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void cancelUpload(@RequestHeader("Authorization") final String accessToken,
      @PathVariable("object-id") String objectId) {
    uploadService.cancelUpload(objectId, uploadService.getUploadId(objectId));
  }

  @RequestMapping(method = RequestMethod.POST, value = "/cancel")
  @ResponseStatus(value = HttpStatus.OK)
  public void cancelAll()
      throws IOException {
    uploadService.cancelAllUpload();
  }
}
