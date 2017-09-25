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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.util.ObjectKeys;
import org.icgc.dcc.storage.server.exception.NotRetryableException;
import org.icgc.dcc.storage.server.repository.BucketNamingService;
import org.icgc.dcc.storage.server.repository.DownloadService;
import org.icgc.dcc.storage.server.repository.URLGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Setter
@Service
@Profile("gcs")
public class GCSDownloadService implements DownloadService{
    @Value("${object.sentinel}")
    private String sentinelObjectId;

    @Autowired
    private URLGenerator urlGenerator;
    @Autowired
    private BucketNamingService bucketNamingService;

    @Value("${collaboratory.data.directory}")
    private String dataDir;

    @Override
    public ObjectSpecification download(String objectId, long offset, long length, boolean forExternalUse) {
        return null;
    }

    @Override
    public String getSentinelObject() {
        if ((sentinelObjectId == null) || (sentinelObjectId.isEmpty())) {
            throw new NotRetryableException(new IllegalArgumentException("Sentinel object id not defined"));
        }
        val now = LocalDateTime.now();
        val expirationDate = Date.from(now.plusMinutes(5).atZone(ZoneId.systemDefault()).toInstant());

        // RV: TODO: Set link to expire to 5 minutes
        return urlGenerator.getDownloadUrl(
                bucketNamingService.getObjectBucketName("", true), ObjectKeys.getObjectKey(dataDir, sentinelObjectId),
                null);
    }
}
