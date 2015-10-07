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

import java.net.URL;

import org.icgc.dcc.storage.fs.util.SeekableURLByteChannel;

import lombok.NonNull;
import lombok.SneakyThrows;

public class StorageSeekableByteChannel extends SeekableURLByteChannel {

  public StorageSeekableByteChannel(@NonNull StoragePath path, @NonNull StorageContext context) {
    super(getUrl(path, context));
  }

  @SneakyThrows
  private static URL getUrl(StoragePath path, StorageContext context) {
    // TODO: Return real URL from Storage Server
    // val context = path.getFileSystem().getProvider().getContext();
    // return context.getUrl(objectId.get());

    //
    // Prototyping
    //

    // Simple extension match, always pointing to the same file
    if (path.endsWith("bam")) {
      return new URL("http://s3.amazonaws.com/iobio/NA12878/NA12878.autsome.bam");
    } else if (path.endsWith("bai")) {
      return new URL("http://s3.amazonaws.com/iobio/NA12878/NA12878.autsome.bam.bai");
    } else if (path.endsWith("json")) {
      return new URL(
          "https://raw.githubusercontent.com/ICGC-TCGA-PanCancer/s3-transfer-operations/master/s3-transfer-jobs-prod1/completed-jobs/001a5fa1-dcc8-43e6-8815-fac34eb8a3c9.RECA-EU.C0015.C0015T.WGS-BWA-Tumor.json");
    } else if (path.getObjectId().isPresent()) {
      return new URL("http://www.google.com");
    }

    return null;
  }

}