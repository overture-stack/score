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
package org.icgc.dcc.storage.server.repository.gcs;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.storage.core.model.ObjectKey;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.icgc.dcc.storage.server.repository.URLGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;


import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Profile("gcs")
public class GCSURLGenerator implements URLGenerator {

    @Autowired
    private ServiceAccountCredentials accountCredentials;
    @Autowired
    private Storage storageInstance;
    @Value("${collaboratory.upload.expiration}")
    private long expirationDays;

    /**
     * Ignore bucketName, uploadId, part and expiration parameters
     */
    @Override
    public String getUploadPartUrl(String bucketName, ObjectKey objectKey, String uploadId, Part part, Date expiration) {
        return generatePresignedUrl(bucketName, objectKey.getObjectId(), HttpMethod.PUT,getExpirationDays(expiration));
    }

    @Override
    public String getDownloadPartUrl(String bucketName, ObjectKey objectKey, Part part, Date expiration) {
        return getDownloadUrl(bucketName, objectKey, expiration);
    }

    @Override
    public String getDownloadUrl(String bucketName, ObjectKey objectKey, Date expiration) {
        return generatePresignedUrl(bucketName, objectKey.getObjectId(), HttpMethod.GET,getExpirationDays(expiration));
    }

    /**
     * Presigned URL for GCS.
     *
     * @param objectId
     * @return
     */
    protected String generatePresignedUrl(String bucketName, String objectId, HttpMethod httpMethod, long expiration) {
        try {
            val blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectId)).build();
            val creds = Storage.SignUrlOption.signWith(accountCredentials);
            val signedUrl = storageInstance.signUrl(blobInfo, expiration, TimeUnit.DAYS, creds,
                    Storage.SignUrlOption.httpMethod(httpMethod));
            return signedUrl.toString();
        } catch (StorageException e) {
            log.error("Unable to generate pre-signed download URL for '{}' due to '{}'", objectId, e.getMessage());
            throw new NotRetryableException(e);
        }
    }

    private long getExpirationDays(Date expiration){
        if(expiration == null) return expirationDays;
        val timeUnit = TimeUnit.MILLISECONDS;
        return timeUnit.convert(Math.abs(new Date().getTime() - expiration.getTime()), TimeUnit.DAYS);
    }

}
