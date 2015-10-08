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
package org.icgc.dcc.storage.fs;

import java.io.IOException;
import java.net.URL;

import org.icgc.dcc.storage.fs.util.SeekableURLByteChannel;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class StorageSeekableByteChannel extends SeekableURLByteChannel {

  private final StoragePath path;

  public StorageSeekableByteChannel(@NonNull StoragePath path, @NonNull StorageContext context) {
    super(getUrl(path, context));
    this.path = path;
  }

  @SneakyThrows
  private static URL getUrl(StoragePath path, StorageContext context) {
    if (path.getFile().isPresent()) {
      val objectId = path.getFile().get().getId();
      val url = context.getUrl(objectId);
      return url;
    }

    return null;
  }

  @Override
  public long size() throws IOException {
    if (path.getFile().isPresent()) {
      return path.getFile().get().getSize();
    }

    return super.size();
  }

}