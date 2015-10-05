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
package org.icgc.dcc.storage.client.fs;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

import javax.naming.OperationNotSupportedException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class StorageSeekableByteChannel implements SeekableByteChannel {

  /**
   * Configuration.
   */
  private final StoragePath path;
  private final Optional<String> objectId;
  private final URL url;

  @Getter(lazy = true)
  private final long size = resolveSize();

  /**
   * State
   */
  private long position;
  private long readPosition;
  private int connectCount;
  private ReadableByteChannel channel;
  private HttpURLConnection connection;

  public StorageSeekableByteChannel(StoragePath path) {
    this.path = path;
    this.position = 0;
    this.objectId = path.getObjectId();
    this.url = getUrl(objectId);
  }

  @Override
  public boolean isOpen() {
    return channel != null;
  }

  @Override
  synchronized public void close() throws IOException {
    channel = null;
    connectCount = 0;
    if (connection != null) {
      connection.disconnect();
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
    if (url == null) {
      return 0;
    }

    return getSize();
  }

  @SneakyThrows
  private long resolveSize() {
    // TODO: Get from dcc-storage-server or dcc-metadata-server
    val urlConnection = (HttpURLConnection) url.openConnection();
    return urlConnection.getContentLengthLong();
  }

  @Override
  synchronized public int read(ByteBuffer buffer) throws IOException {
    if (url == null) {
      return 0;
    }

    log.info("--------------------------------------------------------------");
    try {
      val length = (int) Math.min(buffer.remaining(), size() - position);
      val start = position;
      val end = position + length - 1;
      log.info("Position: {}, Read Position: {}, Buffer: '{}',", position, readPosition, buffer);
      log.info("Reading '{}:{}-{}', Connect Count: {}", url, start, end, connectCount);

      val channel = getChannel();
      val n = channel.read(buffer);
      if (n < 0) {
        System.exit(-1);
      }

      readPosition += n;
      log.info("Read: {}, Read Position: {}", n, readPosition);

      return n;
    } catch (Exception e) {
      log.error("Error reading '{}': {}", path, e);

      throw e;
    }
  }

  @Override
  synchronized public SeekableByteChannel position(long newPosition) throws IOException {
    this.position = newPosition;
    return this;
  }

  @Override
  synchronized public long position() throws IOException {
    return position;
  }

  private ReadableByteChannel getChannel() throws IOException {
    if (connection == null || position != readPosition) {
      if (connection != null) {
        connection.disconnect();
      }

      readPosition = position;

      val range = formatRange(position, size() - 1);

      connectCount++;

      log.info("*** Connect - Range: {}", range);
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoInput(true);
      connection.setRequestProperty("Range", range);
      connection.connect();

      channel = Channels.newChannel(connection.getInputStream());
    }

    return channel;
  }

  @SneakyThrows
  private URL getUrl(Optional<String> objectId) {
    if (!objectId.isPresent()) {
      return null;
    }

    // TODO: Return real URL from Storage Server
    // val fileService = path.getFileSystem().getProvider().getFileService();
    // return fileService.getUrl(objectId.get());

    //
    // Prototyping
    //

    if (path.endsWith("bam")) {
      return new URL("http://s3.amazonaws.com/iobio/NA12878/NA12878.autsome.bam");
    } else if (path.endsWith("bai")) {
      return new URL("http://s3.amazonaws.com/iobio/NA12878/NA12878.autsome.bam.bai");
    } else {
      return new URL("http://www.google.com");
    }
  }

  private static String formatRange(final long start, final long end) {
    return String.format("bytes=%d-%d", start, end);
  }

}