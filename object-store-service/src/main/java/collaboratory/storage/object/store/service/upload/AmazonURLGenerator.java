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
package collaboratory.storage.object.store.service.upload;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import collaboratory.storage.object.store.core.model.Part;
import collaboratory.storage.object.store.core.util.ObjectStoreUtil;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;

/**
 * To generate presigned url for s3-like object storage
 */
public class AmazonURLGenerator implements ObjectURLGenerator {

  @Autowired
  private AmazonS3 s3Client;

  @Override
  public String getUploadPartUrl(String bucketName, String objectKey, String uploadId, Part part, Date expiration) {
    GeneratePresignedUrlRequest req =
        new GeneratePresignedUrlRequest(bucketName, objectKey, HttpMethod.PUT);
    req.setExpiration(expiration);
    req.addRequestParameter("partNumber", String.valueOf(part.getPartNumber()));
    req.addRequestParameter("uploadId", uploadId);
    return s3Client.generatePresignedUrl(req).toString();
  }

  @Override
  public String getDownloadPartUrl(String bucketName, String objectKey, Part part, Date expiration) {
    GeneratePresignedUrlRequest req =
        new GeneratePresignedUrlRequest(bucketName, objectKey, HttpMethod.GET);
    req.putCustomRequestHeader(HttpHeaders.RANGE, ObjectStoreUtil.getRange(part));
    req.setExpiration(expiration);
    return s3Client.generatePresignedUrl(req).toString();
  }
}
