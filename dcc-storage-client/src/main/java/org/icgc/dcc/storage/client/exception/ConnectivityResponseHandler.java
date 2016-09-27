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
package org.icgc.dcc.storage.client.exception;

import static org.icgc.dcc.storage.client.exception.ControlExceptionFactory.notRetryableException;

import java.io.IOException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

@Slf4j
public class ConnectivityResponseHandler extends DefaultResponseErrorHandler {

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    val status = response.getStatusCode();
    switch (status) {
    case FORBIDDEN:
      log.warn("FORBIDDEN response code received. If you are trying to connect to the AWS S3 Repository, you need to be running ths ICGC client on an EC2 VM instance.");
      throw notRetryableException(
          "Amazon S3 error: Access refused by object store. Confirm request host is on permitted list", response);

    case REQUEST_TIMEOUT:
      log.warn("Unable to connect to repository endpoint. You need to be running on a compute node within the repository cloud. Or else your network connection is hinky.");
      throw notRetryableException(
          "Cloud error: Access refused by object store. Confirm request host is on permitted list", response);

    default:
      log.warn("Non-Retryable exception: {}", response.getStatusText());
      throw notRetryableException("Unable to verify connectivity to repository: ", response);

    }
  }
}
