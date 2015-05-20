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
package collaboratory.storage.object.store.service.download;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import collaboratory.storage.object.store.core.model.ObjectSpecification;
import collaboratory.storage.object.store.core.util.ObjectStoreUtil;
import collaboratory.storage.object.store.exception.IdNotFoundException;
import collaboratory.storage.object.store.exception.NotRetryableException;
import collaboratory.storage.object.store.exception.RetryableException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

/**
 * service responsible for object download (full or partial)
 */
@Slf4j
public class ObjectDownloadService {

  @Autowired
  private AmazonS3 s3Client;

  @Value("${collaboratory.bucket.name}")
  private String bucketName;

  @Value("${collaboratory.data.directory}")
  private String dataDir;

  @Value("${s3.endpoint}")
  private String endPoint;

  public ObjectSpecification download(String objectId) {
    String objectMetaKey = ObjectStoreUtil.getObjectMetaKey(dataDir, objectId);

    try {
      GetObjectRequest req =
          new GetObjectRequest(bucketName, objectMetaKey);
      S3Object obj = s3Client.getObject(req);
      ObjectMapper mapper = new ObjectMapper();

      try (S3ObjectInputStream inputStream = obj.getObjectContent()) {
        return mapper.readValue(inputStream, ObjectSpecification.class);
      }
    } catch (AmazonServiceException e) {
      if (e.isRetryable()) {
        throw new RetryableException(e);
      } else {
        throw new IdNotFoundException(objectId);
      }
    } catch (JsonParseException | JsonMappingException e) {
      throw new NotRetryableException(e);
    } catch (IOException e) {
      log.error("Fail to retrieve meta file", e);
      throw new NotRetryableException(e);
    }
  }

  public ObjectSpecification download(String objectId, long offset, long length) {

    return null;
  }

}
