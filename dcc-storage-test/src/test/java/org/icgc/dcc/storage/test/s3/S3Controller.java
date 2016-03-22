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

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static java.util.concurrent.TimeUnit.MINUTES;
import static sirius.kernel.commons.Strings.isFilled;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import sirius.web.controller.Controller;
import sirius.web.controller.Routed;
import sirius.web.http.InputStreamHandler;
import sirius.web.http.WebContext;

@Slf4j
@RequiredArgsConstructor
public class S3Controller extends ninja.S3Controller implements Controller {

  /**
   * Constants.
   */
  private static final int PRIORITY = 0;

  /**
   * Configuration.
   */
  private final boolean block;

  @Override
  @Routed(value = "/s3/:1/:2/**", priority = PRIORITY)
  public void object(WebContext ctx, String bucketName, String objectId, List<String> idList) throws Exception {
    log.info("GET /s3/{}/{}/", bucketName, objectId);
    if (isBlock(ctx)) {
      block();
    }

    super.object(ctx, bucketName, objectId, idList);
  }

  @Override
  @Routed(value = "/s3/:1/:2/**", priority = PRIORITY, preDispatchable = true)
  public void object(WebContext ctx, String bucketName, String objectId, List<String> idList, InputStreamHandler in)
      throws Exception {
    log.info("PUT /s3/{}/{}/", bucketName, objectId);
    super.object(ctx, bucketName, objectId, idList, in);
  }

  @Override
  @Routed(value = "/s3/:1", priority = PRIORITY - 1)
  public void bucket(WebContext ctx, String bucketName) {
    log.info("GET /s3/{}/", bucketName);
    super.bucket(ctx, bucketName);
  }

  @Override
  @Routed(value = "/s3/:1", priority = PRIORITY - 1, preDispatchable = true)
  public void bucket(WebContext ctx, String bucketName, InputStreamHandler in) {
    log.info("PUT /s3/{}/", bucketName);
    super.bucket(ctx, bucketName, in);
  }

  private boolean isBlock(WebContext ctx) {
    return block && isDownload(ctx);
  }

  private boolean isDownload(WebContext ctx) {
    val method = ctx.getRequest().getMethod();
    val uploadId = ctx.get("uploadId").asString();

    return method == GET && !isFilled(uploadId);
  }

  private void block() {
    val minutes = 2;
    log.info("*** Blocking for {} minutes...", minutes);
    sleepUninterruptibly(5, MINUTES);
  }

}
