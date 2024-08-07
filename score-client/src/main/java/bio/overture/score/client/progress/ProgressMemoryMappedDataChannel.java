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
package bio.overture.score.client.progress;

import bio.overture.score.core.model.DataChannel;
import bio.overture.score.core.util.ForwardingDataChannel;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.NonNull;

/***
 * With a memory mapped data channel, instead of paired read/writes over the channel,
 * only one of the two is used, because the other is mapped directly to memory accesses.
 *
 * This means from the standpoint of our status bars, whenever we do one operation, we've already implictly done
 * the other operation successfully at the same time.
 */
public class ProgressMemoryMappedDataChannel extends ForwardingDataChannel {

  private final Progress progress;

  public ProgressMemoryMappedDataChannel(
      @NonNull DataChannel delegate, @NonNull Progress progress) {
    super(delegate);
    this.progress = progress;
  }

  @Override
  public void readFrom(InputStream inputStream) throws IOException {
    super.readFrom(new ProgressMemoryMappedInputStream(inputStream, progress));
  }

  @Override
  public void writeTo(OutputStream outputStream) throws IOException {
    super.writeTo(new ProgressMemoryMappedOutputStream(outputStream, progress));
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
