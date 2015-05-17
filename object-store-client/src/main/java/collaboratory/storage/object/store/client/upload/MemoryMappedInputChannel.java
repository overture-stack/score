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
package collaboratory.storage.object.store.client.upload;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;

/**
 * Channel based on memory mapped buffer
 */
@Slf4j
@AllArgsConstructor
public class MemoryMappedInputChannel extends AbstractInputChannel {

  private final MappedByteBuffer buffer;
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
    try (HashingOutputStream hos = new HashingOutputStream(Hashing.md5(), os)) {
      WritableByteChannel writeChannel = Channels.newChannel(hos);
      writeChannel.write(buffer);
      md5 = hos.hash().toString();
    }
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
  @SuppressWarnings("restriction")
  public void close() {
    Method getCleanerMethod;
    try {
      getCleanerMethod = buffer.getClass().getMethod("cleaner", new Class[0]);
      getCleanerMethod.setAccessible(true);
      sun.misc.Cleaner cleaner = (sun.misc.Cleaner)
          getCleanerMethod.invoke(buffer, new Object[0]);
      cleaner.clean();
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException e) {
      log.warn("fail to unmap memory", e);

    }
  }

}
