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
package collaboratory.storage.object.store.client.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import collaboratory.storage.object.store.client.exception.NotRetryableException;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

/**
 * Channel based on {@link java.nio.MappedByteBuffer memory mapped buffer}
 */
@Slf4j
@AllArgsConstructor
public class MemoryMappedDataChannel extends AbstractDataChannel {

  private MappedByteBuffer buffer;
  private final long offset;
  private final long length;
  private String md5 = null;

  /**
   * it is not possible to reset a memory mapped buffer
   */
  @Override
  public void reset() throws IOException {
    log.debug("resetting buffer to the beginning...");
    buffer.rewind();
  }

  /**
   * Write to a given outputstream and calculate the hash once it is fully written
   */
  @Override
  public void writeTo(OutputStream os) throws IOException {
    try (HashingOutputStream hos = new HashingOutputStream(Hashing.md5(), os)) {
      WritableByteChannel writeChannel = Channels.newChannel(hos);
      writeChannel.write(buffer);
      md5 = hos.hash().toString();
    }
  }

  @Override
  public void writeTo(InputStream is) throws IOException {
    ReadableByteChannel readChannel = Channels.newChannel(is);
    while (buffer.hasRemaining()) {
      readChannel.read(buffer);
    }
  }

  @Override
  public long getLength() {
    return length;
  }

  @Override
  public String getMd5() {
    return md5;
  }

  /**
   * buffer needs to be closed proactively so it won't trigger out-of-memory error
   */
  @Override
  public void commitToDisk() {
    if (!buffer.isDirect()) {
      return;
    }
    // Don't call this because it will slow down
    buffer.force();
    try {
      Method cleanerMethod = buffer.getClass().getMethod("cleaner");
      cleanerMethod.setAccessible(true);
      Object cleaner = cleanerMethod.invoke(buffer);
      Method cleanMethod = cleaner.getClass().getMethod("clean");
      cleanMethod.setAccessible(true);
      cleanMethod.invoke(cleaner);
    } catch (Throwable e) {
      log.error("fail to unmap memory", e);
      throw new NotRetryableException(e);
    } finally {
      buffer = null;
    }
  }
}
