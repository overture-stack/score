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
package org.icgc.dcc.storage.client.manifest;

import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.icgc.dcc.storage.client.manifest.Manifest.ManifestEntry;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class ManifestReader {

  /**
   * Constants.
   */
  private static final Splitter LINE_PARSER = Splitter.on('\t').trimResults();

  @SneakyThrows
  public Manifest readManifest(@NonNull File manifestFile) {
    return readManifest(manifestFile.toURI().toURL());
  }

  @SneakyThrows
  public Manifest readManifest(@NonNull URL manifestFile) {
    return Resources.readLines(manifestFile, UTF_8, new ManifestLineProcessor());
  }

  private static class ManifestLineProcessor implements LineProcessor<Manifest> {

    int count = 0;
    Builder<ManifestEntry> entries = ImmutableList.<ManifestEntry> builder();

    @Override
    public boolean processLine(String line) throws IOException {
      val values = parseLine(line);

      val header = count++ == 0;
      if (!header) {
        val entry = createEntry(values);
        entries.add(entry);
      }

      return true;
    }

    @Override
    public Manifest getResult() {
      return new Manifest(entries.build());
    }
  }

  private static ManifestEntry createEntry(List<String> values) {
    int i = 0;
    return ManifestEntry.builder()
        .repoCode(values.get(i++))
        .fileId(values.get(i++))
        .fileUuid(values.get(i++))
        .fileFormat(values.get(i++))
        .fileName(values.get(i++))
        .fileSize(values.get(i++))
        .fileMd5sum(values.get(i++))
        .indexFileUuid(values.get(i++))
        .donorId(values.get(i++))
        .projectId(values.get(i++))
        .study(values.get(i++))
        .build();
  }

  private static List<String> parseLine(String line) {
    val values = LINE_PARSER.splitToList(line);
    val expectedColumns = 11;
    val actualColumns = values.size();
    checkState(expectedColumns == actualColumns, "Expected %s columns found %s for line '%s'",
        expectedColumns, actualColumns, line);

    return values;
  }

}
