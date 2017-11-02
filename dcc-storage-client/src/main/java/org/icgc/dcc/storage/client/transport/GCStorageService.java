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
package org.icgc.dcc.storage.client.transport;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.dcc.storage.client.exception.NotResumableException;
import org.icgc.dcc.storage.client.exception.NotRetryableException;
import org.icgc.dcc.storage.client.exception.RetryableException;
import org.icgc.dcc.storage.core.model.DataChannel;
import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import static org.springframework.http.HttpMethod.POST;

@Slf4j
@Profile("gcs")
public class GCStorageService extends StorageService {

    /*
        Currently, Google cloud SDK doesn't support resumable uploads. Using Google XML API

     */
    @Override
    public void uploadPart(DataChannel channel, Part part, String objectId, String uploadId) throws IOException {
        retry.execute(new RetryCallback<Void, IOException>() {

            @Override
            public Void doWithRetry(RetryContext ctx) throws IOException {
                log.debug("Upload Part URL: {}", part.getUrl());

                RequestCallback callback = req -> {
                    HttpHeaders requestHeader = req.getHeaders();
                    log.info("Uploading part# {}, length: {}", part.getPartNumber(), channel.getLength());
                    requestHeader.set("content-length", channel.getLength() + "");
                    // set content-range header as per google spec; i.e. 'content-range:bytes 0-524287/*'
                    if(part.getPartNumber() == 2) try {
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //test incorrect order of uploads
                    if(part.getPartNumber() == 3) try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    requestHeader.set("content-range",
                            "bytes "+ part.getOffset() + "-" + (part.getOffset() + channel.getLength() - 1) + "/1083952");
                    try (OutputStream os = req.getBody()) {
                        //channel.writeTo(new FileOutputStream("/Users/rverma/gcs-curl-test/part" + part.getPartNumber()));
                        channel.writeTo(os);
                    }
                };

                try {
                    pingTemplate.execute(new URI(part.getUrl()), HttpMethod.PUT, callback,
                                    response -> response.getHeaders());
                    log.info("Uploaded part# {}", part.getPartNumber());
                } catch (NotResumableException | NotRetryableException e) {
                    log.error("Could not proceed. Failed to send part for part number: {}", part.getPartNumber(), e);
                    throw e;
                } catch (Throwable e) {
                    log.warn("Failed to send part for part #{} : {}", part.getPartNumber(), e);
                    channel.reset();
                    throw new RetryableException(e);
                }
                return null;
            }

        });
    }

    @Override
    public ObjectSpecification initiateUpload(String objectId, long length, boolean overwrite, String md5)
            throws IOException {
        log.debug("Initiating upload, object-id: {} overwrite: {}", objectId, overwrite);
        return retry.execute(ctx -> {
            ObjectSpecification spec = serviceTemplate.exchange(
                    endpoint + "/upload/{object-id}/uploads?fileSize={file-size}&overwrite={overwrite}&md5={checksum}",
                    POST,
                    defaultEntity(),
                    ObjectSpecification.class, objectId, length, overwrite, md5).getBody();
            // initiate the upload and update the parts URL
            String resumableURL = getResumableUploadURL(spec);
            // set resumable URL on each part
            spec.getParts().parallelStream().forEach(part -> part.setUrl(resumableURL));
            return spec;
        });
    }


    @SneakyThrows
    private String getResumableUploadURL(ObjectSpecification spec){
        if(spec.getParts() == null || spec.getParts().size() == 0) {
            log.error("Could not proceed. No parts information available in ObjectSpecification");
            throw new NotRetryableException( new Throwable("No part information available for initializing multi-part upload."));
        }

        // x-goog-resumable header tells google to initialize this upload.
        // Google storage pins this upload in the region from where this call originates
        // i.e. the region where this call is executed
        RequestCallback callback = request -> request.getHeaders().set("x-goog-resumable","start");
        ResponseExtractor<HttpHeaders> headersExtractor = response -> response.getHeaders();

        // the first part contains the signed url
        String signedURL = spec.getParts().get(0).getUrl();
        val signedURLParts = Splitter.on("?").trimResults().splitToList(signedURL);

        try {
            // encode everything after the object id in the URL
            signedURL = Joiner.on("?").join(signedURLParts.get(0),
                    URLEncoder.encode( signedURLParts.get(1), "UTF-8"));
        } catch (UnsupportedEncodingException ueex){
            log.error("Encoding not supported");
        }
        try {

            HttpHeaders responseHeaders =
                    pingTemplate.execute(signedURL, HttpMethod.POST, callback, headersExtractor);
           val resumableURLLocation = responseHeaders.get("Location").get(0);
           // generate the resumable upload URL from the blob path in Signed URL and the returned upload id
            val resumableURL = Joiner.on("?upload_id=").join(
                    signedURLParts.get(0),
                    Splitter.on("upload_id=").trimResults().splitToList(resumableURLLocation).get(1)
            );
            return resumableURL;
        } catch (NotResumableException | NotRetryableException e) {
            log.error("Could not proceed. Failed to send init resumable upload: {}", e);
            throw e;
        } catch (Throwable e) {
            log.warn("Failed to init resumable upload: {}", e);
            throw new RetryableException(e);
        }
    }

    public void finalizeUpload(String objectId, String uploadId) throws IOException {
        log.debug("finalizing upload, object-id: {}, upload-id: {}", objectId, uploadId);
        retry.execute(ctx -> {
            serviceTemplate.exchange(endpoint + "/upload/{object-id}?uploadId={upload-id}", HttpMethod.POST, defaultEntity(),
                    Void.class, objectId, uploadId);
            return null;
        });
        log.debug("finalizing upload returned");
    }

}
