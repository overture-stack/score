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
package org.icgc.dcc.storage.server.service.download;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.core.util.ObjectStoreUtil;
import org.icgc.dcc.storage.server.exception.IdNotFoundException;
import org.icgc.dcc.storage.server.exception.InternalUnrecoverableError;
import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.icgc.dcc.storage.server.exception.RetryableException;
import org.icgc.dcc.storage.server.service.upload.ObjectPartCalculator;
import org.icgc.dcc.storage.server.service.upload.ObjectURLGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

/**
 * service responsible for object download (full or partial)
 */
@Slf4j
@Service
@Setter
public class ObjectDownloadService {

  @Autowired
  private AmazonS3 s3Client;

  @Autowired
  private ObjectURLGenerator urlGenerator;

  @Value("${collaboratory.bucket.name}")
  private String bucketName;

  @Value("${collaboratory.data.directory}")
  private String dataDir;

  @Value("${s3.endpoint}")
  private String endPoint;

  @Value("${collaboratory.download.expiration}")
  private int expiration;

  @Autowired
  ObjectPartCalculator partCalculator;

  public ObjectSpecification download(String objectId, long offset, long length, boolean forExternalUse) {
    checkArgument(offset > -1L);

    // retrieve our meta file for object id
    ObjectSpecification objectSpec = download(objectId);

    // short-circuit in default case
    if (!forExternalUse && (offset == 0L && length < 0L)) {
      return objectSpec;
    }

    // calculate Range values
    // to retrieve to the end of the file
    if (!forExternalUse && (length < 0L)) {
      length = objectSpec.getObjectSize() - offset;
    }

    // validate offset and length parameters:
    // check if the offset + length > length - that would be too big
    if ((offset + length) > objectSpec.getObjectSize()) {
      throw new InternalUnrecoverableError("specified parameters exceed object size (object id: " + objectId
          + ", offset: " + offset
          + ", length: " + length + ")");
    }

    // construct ObjectSpecification for actual object in /data logical folder
    String objectKey = ObjectStoreUtil.getObjectKey(dataDir, objectId);

    List<Part> parts;
    if (forExternalUse) {
      // return as a single part - no matter how large
      parts = partCalculator.specify(0L, -1L);
    }
    else {
      parts = partCalculator.divide(offset, length);
    }
    fillUrl(objectKey, parts, forExternalUse);
    ObjectSpecification spec = new ObjectSpecification(objectKey, objectId, objectId, parts, length);

    return spec;
  }

  // This really is a misleading method name - should be retrieveMetaFile() or something
  public ObjectSpecification download(String objectId) {
    log.debug("Download object id: {}", objectId);
    String objectMetaKey = ObjectStoreUtil.getObjectMetaKey(dataDir, objectId);
    String objectKey = ObjectStoreUtil.getObjectKey(dataDir, objectId);
    log.debug("Download meta key: {}", objectMetaKey);

    try {
      // retrieve .meta file to get list of presigned URL's
      S3Object obj = retrieveObject(objectId, objectMetaKey);

      ObjectMapper mapper = new ObjectMapper();
      ObjectSpecification spec;
      try (S3ObjectInputStream inputStream = obj.getObjectContent()) {
        spec = mapper.readValue(inputStream, ObjectSpecification.class);
        fillUrl(objectKey, spec.getParts(), false);
        return spec;
      }
    } catch (JsonParseException | JsonMappingException e) {
      throw new NotRetryableException(e);

    } catch (IOException e) {
      log.error("Failed to retrieve meta file", e);
      throw new NotRetryableException(e);
    }
  }

  private S3Object retrieveObject(String objectId, String objectKey) {
    try {
      GetObjectRequest req = new GetObjectRequest(bucketName, objectKey);
      return s3Client.getObject(req);
    } catch (AmazonServiceException e) {
      log.error("Exception thrown by Amazon S3", e);
      if (e.getStatusCode() == HttpStatus.NOT_FOUND.value() || !e.isRetryable()) {
        throw new InternalUnrecoverableError(new IdNotFoundException(objectId));
      } else {
        throw new RetryableException(e);
      }
    }
  }

  private void fillUrl(String objectKey, List<Part> parts, boolean forExternalUse) {
    LocalDateTime now = LocalDateTime.now();
    Date expirationDate = Date.from(now.plusDays(expiration).atZone(ZoneId.systemDefault()).toInstant());

    for (Part part : parts) {
      if (forExternalUse) {
        // there should only be one part - don't include RANGE header in pre-signed URL
        part.setUrl(urlGenerator.getDownloadUrl(bucketName, objectKey, expirationDate));
      }
      else {
        part.setUrl(urlGenerator.getDownloadPartUrl(bucketName, objectKey, part, expirationDate));
      }
    }
  }

}
