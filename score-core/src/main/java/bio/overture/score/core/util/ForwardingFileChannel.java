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
package bio.overture.score.core.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class ForwardingFileChannel extends FileChannel {

  private final FileChannel delegate;

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return delegate.equals(obj);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    return delegate.read(dst);
  }

  @Override
  public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
    return delegate.read(dsts, offset, length);
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    return delegate.write(src);
  }

  @Override
  public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
    return delegate.write(srcs, offset, length);
  }

  @Override
  public long position() throws IOException {
    return delegate.position();
  }

  @Override
  public FileChannel position(long newPosition) throws IOException {
    return delegate.position(newPosition);
  }

  @Override
  public long size() throws IOException {
    return delegate.size();
  }

  @Override
  public FileChannel truncate(long size) throws IOException {
    return delegate.truncate(size);
  }

  @Override
  public void force(boolean metaData) throws IOException {
    delegate.force(metaData);
  }

  @Override
  public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
    return delegate.transferTo(position, count, target);
  }

  @Override
  public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
    return delegate.transferFrom(src, position, count);
  }

  @Override
  public int read(ByteBuffer dst, long position) throws IOException {
    return delegate.read(dst, position);
  }

  @Override
  public int write(ByteBuffer src, long position) throws IOException {
    return delegate.write(src, position);
  }

  @Override
  public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
    return delegate.map(mode, position, size);
  }

  @Override
  public FileLock lock(long position, long size, boolean shared) throws IOException {
    return delegate.lock(position, size, shared);
  }

  @Override
  public FileLock tryLock(long position, long size, boolean shared) throws IOException {
    return delegate.tryLock(position, size, shared);
  }

  @Override
  protected void implCloseChannel() throws IOException {
    try {
      val cleanerMethod = delegate.getClass().getMethod("implCloseChannel");
      cleanerMethod.setAccessible(true);
      cleanerMethod.invoke(delegate);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
