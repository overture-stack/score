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
package collaboratory.storage.object.transport;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import collaboratory.storage.object.store.client.upload.NotRetryableException;

import com.google.api.client.util.IOUtils;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

/**
 * Channels that use pipe
 */
@Slf4j
@AllArgsConstructor
public class PipedDataChannel extends AbstractDataChannel {

  private final PipedInputStream is;
  private final long offset;
  private final long length;
  private String md5 = null;

  @Override
  public void reset() throws IOException {
    log.warn("cannot be reset");
    throw new NotRetryableException();
  }

  @Override
  public void writeTo(OutputStream os) throws IOException {
    HashingOutputStream hos = new HashingOutputStream(Hashing.md5(), os);
    IOUtils.copy(is, hos);
    md5 = hos.hash().toString();
  }

  @Override
  public long getlength() {
    return length;
  }

  @Override
  public String getMd5() {
    return md5;
  }

  @Override
  public void close() {
    try {
      is.close();
    } catch (IOException e) {
      log.warn("fail to close the input pipe", e);
    }
  }

}
