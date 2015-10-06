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
package org.icgc.dcc.storage.client.transport;

import java.io.File;
import java.util.List;

import org.icgc.dcc.storage.client.progress.Progress;
import org.icgc.dcc.storage.client.progress.ProgressDataChannel;
import org.icgc.dcc.storage.core.model.DataChannel;
import org.icgc.dcc.storage.core.model.Part;

import com.google.common.base.Preconditions;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * A data transport for sequential upload
 */
@Slf4j
public class SequentialPartObjectTransport implements Transport {

  final private StorageService proxy;
  final private Progress progress;
  final private List<Part> parts;
  final private String objectId;
  final private String uploadId;

  private SequentialPartObjectTransport(SequentialBuilder builder) {
    this.proxy = builder.proxy;
    this.progress = builder.progressBar;
    this.parts = builder.parts;
    this.objectId = builder.objectId;
    this.uploadId = builder.uploadId;
  }

  @Override
  @SneakyThrows
  public void send(File file) {
    progress.start();
    for (Part part : parts) {
      log.debug("processing part: {}", part);
      DataChannel channel =
          new ProgressDataChannel(new FileDataChannel(file, part.getOffset(), part.getPartSize(), null), progress);

      boolean resend = false;
      if (part.getMd5() != null) {
        if (channel.isValidMd5(part.getMd5())) {
          progress.incrementChecksumParts();
          continue;
        }
        proxy.deleteUploadPart(objectId, uploadId, part);
        channel.reset();
        resend = true;
      }

      progress.startTransfer();
      // progress.incrementByteRead(part.getPartSize());
      proxy.uploadPart(channel, part, objectId, uploadId);
      // progress.incrementByteWritten(part.getPartSize());

      if (resend) {
        progress.incrementChecksumParts();
      } else {
        progress.incrementParts(1);
      }
    }
    proxy.finalizeUpload(objectId, uploadId);
    progress.end(false);
  }

  @Override
  public void receive(File file) {
    throw new AssertionError("Please implement it");

  }

  public static Transport.Builder builder() {
    return new SequentialBuilder();
  }

  public static class SequentialBuilder extends Transport.AbstractBuilder {

    @Override
    public Transport build() {
      Preconditions.checkNotNull(parts);
      Preconditions.checkNotNull(proxy);
      Preconditions.checkNotNull(objectId);
      Preconditions.checkNotNull(uploadId);
      Preconditions.checkNotNull(progressBar);
      return new SequentialPartObjectTransport(this);
    }

  }

}
