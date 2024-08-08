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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import bio.overture.score.client.manifest.UploadManifest.ManifestEntry;
import java.io.File;
import lombok.val;
import org.junit.Test;

public class UploadManifestReaderTest {

  @Test
  public void testReadManifest() {
    val reader = new UploadManifestReader();
    val manifest = reader.readManifest(new File("src/test/resources/fixtures/upload/manifest.txt"));

    assertThat(manifest.getEntries(), hasSize(2));
    assertThat(
        manifest.getEntries().get(0),
        equalTo(
            ManifestEntry.builder()
                .fileUuid("00000000-00000000-00000000-00000000")
                .fileName("src/test/resources/fixtures/data/upload.bam")
                .fileMd5sum("f92dc4d0b5d5d98bbb796056139348f9")
                .build()));
    assertThat(
        manifest.getEntries().get(1),
        equalTo(
            ManifestEntry.builder()
                .fileUuid("11111111-00000000-00000000-00000000")
                .fileName("src/test/resources/fixtures/data/upload.bai")
                .fileMd5sum("d16a1aafcf9c22a073a4b8af198707c2")
                .build()));
  }

  @Test(expected = IllegalStateException.class)
  public void testReadPropertiesFormatManifest() {
    val reader = new UploadManifestReader();
    try {
      reader.readManifest(new File("src/test/resources/fixtures/upload/manifest-properties.txt"));
      System.out.println();
    } catch (IllegalStateException e) {
      System.out.println(
          "Invalid Upload manifest file specified. Please check format and ensure it is a 3-column, tab-delimited file. "
              + e.getMessage());
      throw e;
    }
  }
}
