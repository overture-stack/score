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
package org.icgc.dcc.storage.client.exception;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.util.IOUtils;

/**
 * responsible to translate amazon s3 errors to client side errors
 */
@Slf4j
public class AmazonS3RetryableResponseErrorHandler extends DefaultResponseErrorHandler {

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    AmazonS3Exception e = new AmazonS3Exception("s3 exception", IOUtils.toString(response.getBody()));
    switch (response.getStatusCode()) {
    case BAD_REQUEST:
      if (e.getErrorCode() == null || e.getErrorCode().equals("RequestTimeout")) {
        throw new RetryableException(new IOException("Amazon S3 error: "
            + IOUtils.toString(response.getBody())));
      }
    case NOT_FOUND:
      throw new NotRetryableException(new IOException("Amazon S3 error: "
          + IOUtils.toString(response.getBody())));
    case INTERNAL_SERVER_ERROR:
      log.warn("Server error. Stop processing: {}", response.getStatusText());
      throw new NotResumableException(new IOException("Amazon S3 error: "
          + IOUtils.toString(response.getBody())));
    case FORBIDDEN:
      log.warn("FORBIDDEN response code received");
      throw new NotRetryableException(new IOException(
          "Amazon S3 error: Access refused by object store. Confirm request host is on permitted list"
              + IOUtils.toString(response.getBody())));
    default:
      log.warn("Retryable exception: {}", response.getStatusText());
      throw new RetryableException(new IOException("Amazon S3 error: "
          + IOUtils.toString(response.getBody())));

    }
  }
}
