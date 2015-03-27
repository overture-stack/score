/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import javax.ws.rs.QueryParam;

import lombok.Setter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import collaboratory.storage.object.store.core.model.UploadSpecification;
import collaboratory.storage.object.store.service.ObjectUploadService;

@Setter
@RestController
@RequestMapping("/upload")
public class ObjectUploadController {

  @Autowired
  ObjectUploadService uploadService;

  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}/uploads")
  public @ResponseBody
  UploadSpecification initializeMultipartUpload(
      @RequestHeader(value = "access-token", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "fileSize", required = true) long fileSize) {
    return uploadService.initiateUpload(objectId, fileSize);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}/parts")
  public void finalizePartUpload(
      @RequestHeader(value = "access-token", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "partNumber", required = true) int partNumber,
      @RequestParam(value = "uploadId", required = true) String uploadId,
      @RequestParam(value = "md5", required = true) String md5,
      @RequestParam(value = "md5", required = true) String eTag) {
    uploadService.finalizeUploadPart(objectId, uploadId, partNumber, md5, eTag);
  }

  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}")
  public void finalizeUpload(@RequestHeader("access-token") final String accessToken,
      @PathVariable("object-id") String objectId, @QueryParam("uploadId") String uploadId) {
    uploadService.finalizeUpload(objectId, uploadId);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/{object-id}")
  public @ResponseBody
  UploadSpecification getUploadInfo(@RequestHeader("access-token") final String accessToken,
      @PathVariable("object-id") String objectId, @QueryParam("uploadId") String uploadId) {
    return uploadService.getIncompletedUploadParts(objectId, uploadId);
  }

  @RequestMapping(method = RequestMethod.DELETE, value = "/{object-id}")
  public void cancelUpload(@RequestHeader("access-token") final String accessToken,
      @PathVariable("object-id") String objectId, @QueryParam("uploadId") String uploadId) {
    uploadService.cancelUpload(objectId, uploadId);
  }

}
