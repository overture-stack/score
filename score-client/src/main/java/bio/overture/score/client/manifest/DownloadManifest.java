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
package bio.overture.score.client.manifest;

import bio.overture.score.client.command.ViewCommand;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * See https://wiki.oicr.on.ca/display/DCCSOFT/Uniform+metadata+JSON+document+for+ICGC+Data+Repositories#
 * UniformmetadataJSONdocumentforICGCDataRepositories-Manifestfileformatfordownloader
 */
@Value
public class DownloadManifest {
  @NonNull
  private final List<ManifestEntry> entries;

  @Value
  @Builder
  public static class ManifestEntry {

    String repoCode;
    String fileId;
    String fileUuid;
    String fileFormat;
    String fileName;
    String fileSize;
    String fileMd5sum;
    String indexFileUuid;
    String donorId;
    String projectId;
    String study;

    public boolean isBAM() {
      return hasExtention(ViewCommand.OutputFormat.BAM.toString());
    }

    public boolean isCRAM() {
      return hasExtention(ViewCommand.OutputFormat.CRAM.toString());
    }

    public boolean isSAM() {
      return hasExtention(ViewCommand.OutputFormat.SAM.toString());
    }

    public boolean hasExtention(String ext) {
      return fileName.toLowerCase().endsWith(ext.toLowerCase());
    }
  }

  public long getTotalSize() {
    return entries.stream().mapToLong(entry -> Long.valueOf(entry.getFileSize())).sum();
  }
}
