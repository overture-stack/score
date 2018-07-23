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
package bio.overture.score.client.download;

import bio.overture.score.client.metadata.Entity;
import lombok.Builder;
import lombok.Data;
import lombok.val;

import java.io.File;

@Data
@Builder
public class DownloadRequest {

  private File outputDir;
  private String objectId;
  private long offset;
  private long length;
  private boolean validate;
  private Entity entity;
  private OutputLayout layout = OutputLayout.ID;

  public File getOutputFilePath() {
    if (layout == OutputLayout.BUNDLE) {
      // "bundle/filename"
      val bundleDir = new File(outputDir, entity.getGnosId());
      val target = new File(bundleDir, entity.getFileName());
      return target;
    } else if (layout == OutputLayout.FILENAME) {
      // "filename"
      val target = new File(outputDir, entity.getFileName());
      return target;
    } else if (layout == OutputLayout.ID) {
      // "id"
      val target = new File(outputDir, entity.getId());
      return target;
    }
    throw new IllegalStateException("Unsupported layout: " + layout);
  }

}
