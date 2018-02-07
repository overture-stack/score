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
package org.icgc.dcc.score.client.exception;

import static org.icgc.dcc.score.client.exception.ControlExceptionFactory.notResumableException;
import static org.icgc.dcc.score.client.exception.ControlExceptionFactory.notRetryableException;
import static org.icgc.dcc.score.client.exception.ControlExceptionFactory.retryableException;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
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
    HttpStatus status = response.getStatusCode();
    AmazonS3Exception e = new AmazonS3Exception(IOUtils.toString(response.getBody()));
    switch (status) {
    case BAD_REQUEST:
      if (e.getErrorCode() == null || e.getErrorCode().equals("RequestTimeout")) {
        throw retryableException(response);
      }
    case NOT_FOUND:
      throw notRetryableException(response);

    case INTERNAL_SERVER_ERROR:
      log.warn("Server error. Stop processing: {}", response.getStatusText());
      throw notResumableException(response);

    case FORBIDDEN:
      log.warn("FORBIDDEN response code received");
      throw notRetryableException(
          "Access refused by object store. Confirm client is part of repository cloud and that the download was initiated less than 1 day earlier (7 days for uploads)",
          response);

    default:
      log.warn("Retryable exception: {}", response.getStatusText());
      throw retryableException(response);

    }
  }
}
