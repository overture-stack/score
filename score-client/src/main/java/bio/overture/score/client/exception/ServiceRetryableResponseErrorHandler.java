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
package bio.overture.score.client.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

import static bio.overture.score.client.exception.ControlExceptionFactory.*;

/**
 * Responsible for translating server-side errors to client-side errors
 */
@Slf4j
public class ServiceRetryableResponseErrorHandler extends DefaultResponseErrorHandler {

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    switch (response.getStatusCode()) {
    case NOT_FOUND:
    case BAD_REQUEST:
      log.warn("Bad request. Stop processing: {}", response.getStatusText());
      throw notRetryableException("Storage client error: ", response);

    case INTERNAL_SERVER_ERROR:
      log.warn("Server error. Stop processing: {}", response.getStatusText());
      throw notResumableException("Storage client error: ", response);

    case UNAUTHORIZED:
      log.warn("Unauthorized. Stop processing: {}", response.getStatusText());
      throw notResumableException("Storage client error: ", response);

    case FORBIDDEN:
      log.warn("Access Denied. Stop processing: {}", response.getStatusText());
      throw notResumableException("Storage client error: ", response);

    default:
      log.warn("Retryable exception: {}", response.getStatusText());
      throw retryableException("Storage server error: ", response);
    }
  }
}
