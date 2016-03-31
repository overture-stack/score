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
package org.icgc.dcc.storage.client.manifest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.icgc.dcc.storage.client.manifest.Manifest.ManifestEntry;
import org.junit.Test;

import lombok.val;

public class ManifestReaderTest {

  @Test
  public void testReadManifest() {
    val reader = new ManifestReader();
    val manifest = reader.readManifest(new File("src/test/resources/fixtures/download/manifest.txt"));

    assertThat(manifest.getEntries(), hasSize(2));
    assertThat(manifest.getEntries().get(0), equalTo(ManifestEntry.builder()
        .repoCode("1")
        .fileId("2")
        .fileUuid("3")
        .fileFormat("4")
        .fileName("5")
        .fileSize("6")
        .fileMd5sum("7")
        .indexFileUuid("8")
        .donorId("9")
        .projectId("10")
        .study("11")
        .build()));
    assertThat(manifest.getEntries().get(1), equalTo(ManifestEntry.builder()
        .repoCode("11")
        .fileId("10")
        .fileUuid("9")
        .fileFormat("8")
        .fileName("7")
        .fileSize("6")
        .fileMd5sum("5")
        .indexFileUuid("4")
        .donorId("3")
        .projectId("2")
        .study("1")
        .build()));

  }

}
