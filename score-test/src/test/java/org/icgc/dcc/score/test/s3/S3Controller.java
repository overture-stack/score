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
package org.icgc.dcc.score.test.s3;

import static java.util.stream.Collectors.joining;
import static org.icgc.dcc.score.test.s3.S3Request.Resource.BUCKET;
import static org.icgc.dcc.score.test.s3.S3Request.Resource.OBJECT;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import sirius.kernel.commons.Strings;
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
  private static final int PRIORITY = 0; // Needs to be lower than all @Routed priorities in super
  private static final Function<S3Request, Boolean> DEFAULT_HANDLER = (request) -> false;

  /**
   * Configuration.
   */
  private final AtomicReference<Function<S3Request, Boolean>> handler = new AtomicReference<>(DEFAULT_HANDLER);

  public void setHandler(@NonNull Function<S3Request, Boolean> handler) {
    this.handler.set(handler);
  }

  public void unsetHandler() {
    setHandler(DEFAULT_HANDLER);
  }

  @Override
  @Routed(value = "/s3/:1/:2/**", priority = PRIORITY)
  public void object(WebContext ctx, String bucketName, String objectId, List<String> idList) throws Exception {
    log.info("GET /s3/{}/{}/", bucketName, objectId);
    val request = new S3Request(ctx, OBJECT, bucketName, resolveObjectId(objectId, idList));
    if (handle(request)) {
      return;
    }

    super.object(ctx, bucketName, objectId, idList);
  }

  @Override
  @Routed(value = "/s3/:1/:2/**", priority = PRIORITY, preDispatchable = true)
  public void object(WebContext ctx, String bucketName, String objectId, List<String> idList, InputStreamHandler in)
      throws Exception {
    log.info("PUT /s3/{}/{}/", bucketName, objectId);
    val request = new S3Request(ctx, OBJECT, bucketName, resolveObjectId(objectId, idList));
    if (handle(request)) {
      return;
    }

    super.object(ctx, bucketName, objectId, idList, in);
  }

  @Override
  @Routed(value = "/s3/:1", priority = PRIORITY - 1)
  public void bucket(WebContext ctx, String bucketName) {
    log.info("GET /s3/{}/", bucketName);
    val request = new S3Request(ctx, BUCKET, bucketName, null);
    if (handle(request)) {
      return;
    }

    super.bucket(ctx, bucketName);
  }

  @Override
  @Routed(value = "/s3/:1", priority = PRIORITY - 1, preDispatchable = true)
  public void bucket(WebContext ctx, String bucketName, InputStreamHandler in) {
    log.info("PUT /s3/{}/", bucketName);
    val request = new S3Request(ctx, BUCKET, bucketName, null);
    if (handle(request)) {
      return;
    }

    super.bucket(ctx, bucketName, in);
  }

  private Boolean handle(S3Request request) {
    return handler.get().apply(request);
  }

  private static String resolveObjectId(String objectId, List<String> idList) {
    val ids = ImmutableList.<String> builder().add(objectId).addAll(idList).build();
    return ids.stream().filter(Strings::isFilled).collect(joining("/"));
  }

}
