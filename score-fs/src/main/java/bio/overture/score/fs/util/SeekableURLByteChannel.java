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
package bio.overture.score.fs.util;

import static com.google.common.net.HttpHeaders.RANGE;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import javax.naming.OperationNotSupportedException;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A read-only {@link SeekableByteChannel} implementation that is backed by an {@link HttpURLConnection}.
 * <p>
 * The connection should support the HTTP {@code Range} header in order to support random access into the remote
 * resource at the specified {@link #url}.
 */
@Slf4j
public class SeekableURLByteChannel implements SeekableByteChannel {

  /**
   * Constants.
   */
  private static final int READ_TIMEOUT_MS = (int) SECONDS.toMillis(30);

  /**
   * Configuration.
   */
  protected URL url;

  @Getter(lazy = true)
  private final long size = resolveSize();

  /**
   * State - Metrics
   */
  protected long position;
  protected long lastPosition;
  protected int connectCount;
  protected int byteCount;

  /**
   * State - Data
   */
  protected InputStream inputStream;
  protected HttpURLConnection connection;

  public SeekableURLByteChannel(@NonNull URL url) {
    this.url = url;
  }

  @Override
  public boolean isOpen() {
    return inputStream != null;
  }

  @Override
  synchronized public void close() throws IOException {
    inputStream = null;
    if (connection != null) {
      try {
        connection.disconnect();
      } catch (Exception e) {
        log.error("Exception closing connection: ", e);
      }
      connection = null;
    }
  }

  @Override
  @SneakyThrows
  public int write(ByteBuffer src) throws IOException {
    throw new OperationNotSupportedException();
  }

  @Override
  @SneakyThrows
  public SeekableByteChannel truncate(long size) throws IOException {
    throw new OperationNotSupportedException();
  }

  @Override
  public long size() throws IOException {
    return getSize();
  }

  @Override
  synchronized public int read(ByteBuffer buffer) throws IOException {
    if (buffer.remaining() == 0) {
      // Nothing to fill
      return 0;
    }

    if (position == size()) {
      // EOF
      return -1;
    }

    try {
      val length = (int) Math.min(buffer.remaining(), size() - position);
      val start = position;
      val end = position + length - 1;

      log.debug("--------------------------------------------------------------");
      log.debug("Current position: {}, Last position: {}, Buffer: '{}',", position, lastPosition, buffer);
      log.debug("--------------------------------------------------------------");

      log.debug("Reading range '{}:{}-{}', Connect count: {}", url, start, end, connectCount);
      val n = read(buffer, length);
      byteCount += n;
      position += n;
      lastPosition = position;
      log.debug("Read bytes: {}, Current position: {}", n, position);

      return n;
    } catch (Exception e) {
      log.error("Error reading '{}': {}", url, e);

      throw e;
    }
  }

  @Override
  synchronized public SeekableByteChannel position(long newPosition) throws IOException {
    lastPosition = position;
    position = newPosition;

    return this;
  }

  @Override
  synchronized public long position() throws IOException {
    return position;
  }

  /**
   * Template method.
   */
  protected void onResolveInputStream() throws IOException {
    // No-op
  }

  private int read(ByteBuffer buffer, int length) throws IOException, EOFException {
    val inputStream = resolveInputStream();

    int n = 0;
    byte[] bytes = new byte[length];
    while (n < length) {
      // TODO: Reconnect on timeout?
      int count = inputStream.read(bytes, n, length - n);
      if (count < 0) throw new EOFException();
      n += count;
    }

    buffer.put(bytes);

    return n;
  }

  @SneakyThrows
  private long resolveSize() {
    val connection = (HttpURLConnection) url.openConnection();
    return connection.getContentLengthLong();
  }

  private InputStream resolveInputStream() throws IOException {
    onResolveInputStream();

    // Is new or non-serial read relative to the last read request
    val reconnect = connection == null || position != lastPosition;
    if (reconnect) {
      if (connection != null) {
        connection.disconnect();
      }

      val range = formatRange(position, size() - 1);
      log.debug("*** Connect - Range: {}", range);

      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestProperty(RANGE, range);
      connection.setReadTimeout(READ_TIMEOUT_MS);
      connection.connect();

      inputStream = connection.getInputStream();
      connectCount++;
    }

    return inputStream;
  }

  private static String formatRange(long start, long end) {
    return String.format("bytes=%d-%d", start, end);
  }

}