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
package org.icgc.dcc.storage.client.progress;

import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.icgc.dcc.storage.core.util.ForwardingFileChannel;

public class ProgressFileChannel extends ForwardingFileChannel {

  private final Progress progress;

  public ProgressFileChannel(@NonNull FileChannel delegate, @NonNull Progress progress) {
    super(delegate);
    this.progress = progress;
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    val value = super.read(dst);
    if (value > 0) {
      progress.incrementBytesRead(value);
    }

    return value;
  }

  @Override
  public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
    val value = super.read(dsts, offset, length);
    if (value > 0) {
      progress.incrementBytesRead(value);
    }

    return value;
  }

  @Override
  public int read(ByteBuffer dst, long position) throws IOException {
    val value = super.read(dst, position);
    if (value > 0) {
      progress.incrementBytesRead(value);
    }

    return value;
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    val value = super.write(src);
    if (value > 0) {
      progress.incrementBytesWritten(value);
    }

    return value;
  }

  @Override
  public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
    val value = super.write(srcs, offset, length);
    if (value > 0) {
      progress.incrementBytesWritten(value);
    }

    return value;
  }

  @Override
  public int write(ByteBuffer src, long position) throws IOException {
    val value = super.write(src, position);
    if (value > 0) {
      progress.incrementBytesWritten(value);
    }

    return value;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

}
