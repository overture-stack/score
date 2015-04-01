/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package collaboratory.storage.object.store.client.cli.command;

import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import lombok.val;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

@Component
@Parameters(separators = "=", commandDescription = "Upload Abort")
public class AbortCommand extends AbstractClientCommand {

  /**
   * 
   */
  private static final int SEVEN_DAYS = 1000 * 60 * 60 * 24 * 7;

  @Parameter(names = "--keys", description = "listing", required = false)
  private List<String> keys = ImmutableList.of();

  @Parameter
  private List<String> bucketNames;

  @Parameter(names = "-e", description = "abort all part uploads a week ago", required = false)
  private boolean abortPrevious;

  @Autowired
  private AmazonS3 s3Client;

  private static final Predicate<MultipartUpload> DEFAULT_PREDICATE = new Predicate<MultipartUpload>() {

    @Override
    public boolean apply(@Nullable MultipartUpload input) {
      return true;
    }
  };

  @Override
  public int execute() {

    if (!keys.isEmpty()) {
      // TODO: abort those uploads
      for (val bucketName : bucketNames) {
        Iterable<MultipartUpload> abortIds = list(bucketName, new Predicate<MultipartUpload>() {

          @Override
          public boolean apply(@Nullable MultipartUpload input) {
            return keys.contains(input.getKey());
          }
        });

        for (val id : abortIds) {
          System.err.println("Aborting multi-parts for: " + id);
          s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(
              bucketName, id.getKey(), id.getUploadId()));
        }
      }
    } else if (abortPrevious) {
      TransferManager tm = new TransferManager(new ProfileCredentialsProvider());
      Date oneWeekAgo = new Date(System.currentTimeMillis() - SEVEN_DAYS);
      for (val bucketName : bucketNames) {
        System.err.println("Aborting all multi-parts in: " + bucketName);
        tm.abortMultipartUploads(bucketName, oneWeekAgo);
      }
    } else {
      for (val bucketName : bucketNames) {
        Iterable<MultipartUpload> parts = list(bucketName, DEFAULT_PREDICATE);
        for (val part : parts) {
          System.err.println("Upload ID: " + part.getUploadId() + ", Key: " + part.getKey());
        }
      }
    }
    return SUCCESS_STATUS;
  }

  private Iterable<MultipartUpload> list(String bucketName, Predicate<MultipartUpload> predicate) {
    ListMultipartUploadsRequest allMultpartUploadsRequest =
        new ListMultipartUploadsRequest(bucketName);
    MultipartUploadListing multipartUploadListing =
        s3Client.listMultipartUploads(allMultpartUploadsRequest);
    return Iterables.filter(multipartUploadListing.getMultipartUploads(), predicate);
  }
}