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
package org.icgc.dcc.storage.server.controller;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.server.repository.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.common.io.ByteStreams;
import com.google.common.io.CountingInputStream;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * An emulator for S3 upload for benchmarking purpose only
 */
@Setter
@RestController
@RequestMapping("/upload")
@Slf4j
@Profile("benchmark")
public class BenchmarkUploadController extends UploadController {

  @Autowired
  UploadService uploadService;

  @Override
  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}/uploads")
  public @ResponseBody ObjectSpecification initializeMultipartUpload(
      @RequestHeader(value = "access-token", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "overwrite", required = false, defaultValue = "false") boolean overwrite,
      @RequestParam(value = "fileSize", required = true) long fileSize,
      @RequestParam(value = "md5", required = false) String md5,
      @RequestHeader(value = "User-Agent", defaultValue = "unknown") String userAgent,
      HttpServletRequest request) {
    return uploadService.initiateUpload(objectId, fileSize, md5, overwrite);
  }

  @Override
  @RequestMapping(method = RequestMethod.DELETE, value = "/{object-id}/parts")
  @ResponseStatus(value = HttpStatus.OK)
  public void deletePart(
      @RequestHeader(value = "access-token", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "partNumber", required = true) int partNumber,
      @RequestParam(value = "uploadId", required = true) String uploadId,
      @RequestHeader(value = "User-Agent", defaultValue = "unknown") String userAgent,
      HttpServletRequest request) {
    // NO-OP
  }

  @Override
  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}/parts")
  @ResponseStatus(value = HttpStatus.OK)
  public void finalizePartUpload(
      @RequestHeader(value = "access-token", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "partNumber", required = true) int partNumber,
      @RequestParam(value = "uploadId", required = true) String uploadId,
      @RequestParam(value = "md5", required = true) String md5,
      @RequestParam(value = "etag", required = true) String eTag) {
    // NO-OP
  }

  @Override
  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void finalizeUpload(
      @RequestHeader(value = "access-token", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "uploadId", required = true) String uploadId) {
    // NO-OP
  }

  @Override
  @RequestMapping(method = RequestMethod.POST, value = "/{object-id}/recovery")
  @ResponseStatus(value = HttpStatus.OK)
  public void tryRecover(
      @RequestHeader(value = "access-token", required = true) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "fileSize", required = true) long fileSize) {
    // NO-OP
  }

  @RequestMapping(method = RequestMethod.PUT, value = "/data/{object-id}")
  public ResponseEntity<?> nullInputStream(
      @PathVariable("object-id") String objectId,
      @RequestParam(value = "partNumber", required = true) int partNumber,
      @RequestParam(value = "uploadId", required = true) String uploadId,
      InputStream is) throws IOException {

    try (HashingOutputStream hos = new HashingOutputStream(Hashing.md5(), ByteStreams.nullOutputStream())) {
      CountingInputStream counterStream = new CountingInputStream(is);
      IOUtils.copyLarge(counterStream, hos);
      log.info("object id: {}, part number: {}, upload id: {}, number of bytes: {}", objectId, partNumber, uploadId,
          counterStream.getCount());
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.setETag("\"" + hos.hash().toString() + "\"");
      return new ResponseEntity<>(null, httpHeaders, HttpStatus.CREATED);
    }

  }

}
