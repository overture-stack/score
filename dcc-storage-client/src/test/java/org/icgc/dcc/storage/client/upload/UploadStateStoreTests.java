package org.icgc.dcc.storage.client.upload;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import lombok.val;

import org.icgc.dcc.storage.client.upload.UploadStateStore;
import org.junit.Test;

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

/**
 * 
 */
public class UploadStateStoreTests {

  @Test
  public void test_fetch_upload_id_finds_file() throws IOException {
    val dir =
        new File(getClass().getClassLoader().getResource("fixtures/upload/placeholder-upload-file.txt").getFile());
    Optional<String> result = UploadStateStore.fetchUploadId(dir, "valid-upload-id");
    if (result.isPresent()) {
      System.out.println(result.get());
    } else {
      System.out.println("not there");
    }
    assertEquals("this-is-my-uploadId", result.get());
  }

  @Test
  public void test_fetch_upload_id_finds_improperly_formatted_file() throws IOException {
    val dir =
        new File(getClass().getClassLoader().getResource("fixtures/upload/placeholder-upload-file.txt").getFile());
    Optional<String> result = UploadStateStore.fetchUploadId(dir, "invalid-upload-id");
    assertFalse(result.isPresent());
  }
}
