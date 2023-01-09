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
package bio.overture.score.server.repository;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

import bio.overture.score.core.model.ObjectKey;
import bio.overture.score.core.model.Part;
import org.springframework.beans.factory.annotation.Value;

/**
 * To generate url for benchmarking
 */
@Slf4j
public class BenchmarkURLGenerator implements URLGenerator {

  @Value("${benchmark.endpoint}")
  private String endpoint;

  @Override
  public String getUploadPartUrl(String bucketName, ObjectKey objectKey, String uploadId, Part part, Date expiration) {
    log.info("Benchmark mode is on");
    return endpoint + "/upload/" + objectKey + "?partNumber=" + String.valueOf(part.getPartNumber()) + "&uploadId="
        + uploadId;
  }

  @Override
  public String getDownloadPartUrl(String bucketName, ObjectKey objectKey, Part part, Date expiration) {
    return null;
  }

  @Override
  public String getDownloadUrl(String bucketName, ObjectKey objectKey, Date expiration) {
    return null;
  }
}
