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
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;

import javax.naming.OperationNotSupportedException;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@AllArgsConstructor
public class StorageSeekableByteChannel implements SeekableByteChannel {

  private final StoragePath path;
  private final Optional<String> objectId;
  private final URL url;
  private long position;

  public StorageSeekableByteChannel(StoragePath path) {
    this.path = path;
    this.position = 0;
    this.objectId = path.getObjectId();
    this.url = getUrl(objectId);
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public void close() throws IOException {
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

    val urlConnection = (HttpURLConnection) url.openConnection();
    return urlConnection.getContentLengthLong();
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (url == null) {
      return 0;
    }

    val length = (int) Math.min(dst.remaining(), size() - position);
    val start = position;
    val end = position + length - 1;
    val range = start + "-" + end;

    val urlConnection = (HttpURLConnection) url.openConnection();
    urlConnection.setDoInput(true);
    urlConnection.setRequestProperty("Range", "bytes=" + range);
    urlConnection.connect();

    val channel = Channels.newChannel(urlConnection.getInputStream());
    return channel.read(dst);
  }

  @SneakyThrows
  private URL getUrl(Optional<String> objectId) {
    if (!objectId.isPresent()) {
      return null;
    }

    // TODO: Return real URL from Storage Server
    // val fileService = path.getFileSystem().getProvider().getFileService();
    // return fileService.getUrl(objectId.get());

    if (path.endsWith("bam")) {
      return new URL("http://s3.amazonaws.com/iobio/NA12878/NA12878.autsome.bam");
    } else if (path.endsWith("bai")) {
      return new URL("http://s3.amazonaws.com/iobio/NA12878/NA12878.autsome.bam.bai");
    } else {
      return new URL("http://www.google.com");
    }
  }

  @Override
  public SeekableByteChannel position(long newPosition) throws IOException {
    this.position = newPosition;
    return this;
  }

  @Override
  public long position() throws IOException {
    return position;
  }

}