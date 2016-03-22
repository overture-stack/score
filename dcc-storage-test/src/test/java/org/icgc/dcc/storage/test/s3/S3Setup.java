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
package org.icgc.dcc.storage.test.s3;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;

import java.io.File;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import sirius.kernel.Setup;

public class S3Setup extends Setup {

  private Config config = ConfigFactory.empty();

  public S3Setup(ClassLoader loader) {
    // NOTE: Mode.TEST will use a hard-coded working directory, ignoring the baseDir config parameter
    super(Mode.PROD, loader);
  }

  /**
   * Base dir for file storage. By default it's the s3 subdirectory from the projects root folder.
   */
  public S3Setup withBaseDir(File baseDir) {
    set("storage.baseDir", baseDir.getAbsolutePath());
    return this;
  }

  public S3Setup withMultipartDir(File multipartDir) {
    set("storage.multipartDir", multipartDir.getAbsolutePath());
    return this;
  }

  /**
   * Will buckets be auto created on the first request via the S3 API?
   */
  public S3Setup withAutoCreateBuckets(boolean autoCreate) {
    set("storage.autocreateBuckets", autoCreate);
    return this;
  }

  @Override
  public Config loadInstanceConfig() {
    return config;
  }

  @Override
  protected String getLogsDirectory() {
    return "target/test/logs";
  }

  @Override
  protected String getLogFileName() {
    return "s3.log";
  }

  private void set(String name, Object value) {
    config = config.withValue(name, fromAnyRef(value));
  }

}
