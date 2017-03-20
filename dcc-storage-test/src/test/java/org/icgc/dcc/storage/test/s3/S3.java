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

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.Getter;
import lombok.val;
import sirius.kernel.Setup;
import sirius.kernel.Sirius;
import sirius.kernel.di.Injector;
import sirius.web.controller.Controller;

public class S3 {

  @Getter
  private File bucketDir;

  @Getter
  private File multipartDir;

  public void start(File s3Root) {
    val setup = createSetup(s3Root);

    Sirius.start(setup);

    registerController();
  }

  public void onRequest(Function<S3Request, Boolean> handler) {
    getController().setHandler(handler);
  }

  public void onRequest(Consumer<S3Request> handler) {
    onRequest((request) -> {
      handler.accept(request);

      // Continue
      return false;
    });
  }

  public void reset() {
    getController().unsetHandler();
  }

  public void stop() {
    Sirius.stop();
  }

  private Setup createSetup(File s3Root) {
    bucketDir = new File(s3Root, "buckets");
    checkState(bucketDir.mkdir(), "Could not create dir: %s", bucketDir);

    multipartDir = new File(s3Root, "multipart");
    checkState(multipartDir.mkdir(), "Could not create dir: %s", multipartDir);

    return new S3Setup(ClassLoader.getSystemClassLoader())
        .withAutoCreateBuckets(true)
        .withBaseDir(bucketDir)
        .withMultipartDir(multipartDir)
        .withLogToFile(true)
        .withLogToConsole(true);
  }

  private static void registerController() {
    val controller = Injector.context().wire(new S3Controller());
    Injector.context().registerDynamicPart(S3Controller.class.getName(), controller, Controller.class);
  }

  private static S3Controller getController() {
    return (S3Controller) Injector.context().findPart(S3Controller.class.getName(), Controller.class);
  }

}
