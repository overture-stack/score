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

  private final StoragePath path;
  private final Optional<String> objectId;
  private final URL url;
  private long position;
  private long readPosition;

  @Getter(lazy = true)
  private final long size = resolveSize();

  private ReadableByteChannel channel;

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
  public void close() throws IOException {
    channel = null;
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

    try {
      val length = (int) Math.min(buffer.remaining(), size() - position);
      val start = position;
      val end = position + length - 1;
      log.info("Position: {}, Buffer  '{}'", position, buffer);
      log.info("Reading '{}:{}-{}'", url, start, end);

      val channel = getChannel(start, end);
      return channel.read(buffer);
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

  private ReadableByteChannel getChannel(long start, long end) throws IOException {
    if (channel == null || start != readPosition + 1) {
      if (channel != null) {
        channel.close();
      }

      val urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setDoInput(true);
      urlConnection.setRequestProperty("Range", formatRange(start, end));
      urlConnection.connect();

      channel = Channels.newChannel(urlConnection.getInputStream());
    }

    readPosition = end;

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