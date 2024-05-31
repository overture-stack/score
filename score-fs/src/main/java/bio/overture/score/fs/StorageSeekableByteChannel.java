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
package bio.overture.score.fs;

import bio.overture.score.fs.util.SeekableURLByteChannel;
import java.io.IOException;
import java.net.URL;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class StorageSeekableByteChannel extends SeekableURLByteChannel {

  /** Configuration. */
  private final StoragePath path;

  /** Dependencies */
  private final StorageContext context;

  public StorageSeekableByteChannel(@NonNull StoragePath path, @NonNull StorageContext context) {
    super(getUrl(path, context));
    this.path = path;
    this.context = context;
  }

  @Override
  protected void onResolveInputStream() throws IOException {
    val currentUrl = getUrl(path, context);
    val timeout = url != currentUrl;
    if (timeout) {
      // Close the connection and assign new URL with extended timeout
      close();
      this.url = currentUrl;
    }
  }

  @SneakyThrows
  private static URL getUrl(StoragePath path, StorageContext context) {
    if (path.getFile().isPresent()) {
      val objectId = path.getFile().get().getObjectId();
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

  @Override
  public synchronized void close() throws IOException {
    super.close();
    context.incrementCount("connectCount", connectCount);
    context.incrementCount("byteCount", byteCount);
  }
}
