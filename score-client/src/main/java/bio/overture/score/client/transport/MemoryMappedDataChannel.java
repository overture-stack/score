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
package bio.overture.score.client.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import bio.overture.score.client.exception.NotRetryableException;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

/**
 * Channel based on {@link java.nio.MappedByteBuffer memory mapped buffer}
 */
@Slf4j
@AllArgsConstructor
public class MemoryMappedDataChannel extends AbstractDataChannel {

  private MappedByteBuffer buffer;
  @Getter
  private final long offset;
  @Getter
  private final long length;

  @Getter
  private String md5 = null;

  /**
   * it is not possible to reset a memory mapped buffer
   */
  @Override
  public void reset() throws IOException {
    log.debug("Resetting buffer to the beginning...");
    buffer.rewind();
  }

  /**
   * Write to a given output stream and calculate the hash once it is fully written
   */
  @Override
  public void writeTo(OutputStream os) throws IOException {
    try (val hos = new HashingOutputStream(Hashing.md5(), os)) {
      val writeChannel = Channels.newChannel(hos);
      writeChannel.write(buffer);
      md5 = hos.hash().toString();
    }
  }

  @Override
  public void readFrom(InputStream is) throws IOException {
    val readChannel = Channels.newChannel(is);

    while (buffer.hasRemaining()) {
      val eos = readChannel.read(buffer) < 0;
      if (eos) {
        // This end-of-stream check put in as part of COL-492:
        // Since the while-loop condition is actually on the buffer that is receiving the data from
        // the input channel continuing to have space, and the input channel is the body of an HttpResponse
        // that would be subject to network interruptions. Then in the event that the stream gets interrupted
        // part-way through the stream, the while loop will never exit because the buffer will continue to
        // have the same number of elements (space) remaining; there is no more input coming into it.

        // Since this is a hypothesis, we have additional insurance from the fact that the InputStream being passed
        // into the channel is actually a HashingInputStream. When readFrom() returns via the break, the hash that
        // was calculated until that point will be returned and validated against the expected MD5 for the entire part.
        // This IllegalStateException is caught and re-thrown as a RetryableException.
        log.warn("Reached end of stream while trying to fill buffer: {}", buffer);
        break;
      }
    }
  }

  /**
   * Buffer needs to be closed proactively so it won't trigger out-of-memory error
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
