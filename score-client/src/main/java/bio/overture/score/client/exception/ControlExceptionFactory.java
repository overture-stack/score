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

import com.amazonaws.util.IOUtils;
import java.io.IOException;
import org.springframework.http.client.ClientHttpResponse;

public class ControlExceptionFactory {

  public static RetryableException retryableException(ClientHttpResponse response)
      throws IOException {
    return retryableException(null, response);
  }

  public static RetryableException retryableException(String prefix, ClientHttpResponse response)
      throws IOException {
    return new RetryableException(
        new IOException((prefix == null ? "" : prefix) + IOUtils.toString(response.getBody())));
  }

  public static NotRetryableException notRetryableException(ClientHttpResponse response)
      throws IOException {
    return notRetryableException(null, response);
  }

  public static NotRetryableException notRetryableException(
      String prefix, ClientHttpResponse response) throws IOException {
    return new NotRetryableException(
        new IOException((prefix == null ? "" : prefix) + IOUtils.toString(response.getBody())));
  }

  public static NotResumableException notResumableException(ClientHttpResponse response)
      throws IOException {
    return notResumableException(null, response);
  }

  public static NotResumableException notResumableException(
      String prefix, ClientHttpResponse response) throws IOException {
    return new NotResumableException(
        new IOException((prefix == null ? "" : prefix) + IOUtils.toString(response.getBody())));
  }
}
