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
package org.icgc.dcc.storage.test.s3;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;

import lombok.val;
import sirius.kernel.Setup;
import sirius.kernel.Sirius;

public class S3 {

  public void start(File s3Root) {
    val setup = createSetup(s3Root);

    Sirius.start(setup);
  }

  public void stop() {
    Sirius.stop();
  }

  private Setup createSetup(File s3Root) {
    val baseDir = new File(s3Root, "buckets");
    checkState(baseDir.mkdir(), "Could not create dir: %s", baseDir);

    val multipartDir = new File(s3Root, "multipart");
    checkState(multipartDir.mkdir(), "Could not create dir: %s", multipartDir);

    return new S3Setup(ClassLoader.getSystemClassLoader())
        .withAutoCreateBuckets(true)
        .withBaseDir(baseDir)
        .withMultipartDir(multipartDir)
        .withLogToFile(true)
        .withLogToConsole(true);
  }

}
