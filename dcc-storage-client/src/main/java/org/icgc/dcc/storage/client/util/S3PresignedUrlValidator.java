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
package org.icgc.dcc.storage.client.util;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.http.HttpHeaders;

import com.amazonaws.auth.internal.SignerConstants;

@Slf4j
public class S3PresignedUrlValidator extends PresignedUrlValidator {

  // Extended format uses hyphens. Named formatters in Java 8 (like ISO_INSTANT) are extended format only
  // So, we have to define formatter with explicit ISO 8601 basic format
  private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");

  /**
   * Return expiry date of presigned URL in the local time zone (system default). Distinguished between V2 and V4 AWS
   * signatures
   * @param args - URL query arguments
   * @return expiry date translated to specified time zone
   */
  @Override
  LocalDateTime extractExpiryDate(Map<String, String> args, ZoneId effectiveTimeZone) {
    LocalDateTime expiry = null;
    if (args.containsKey(HttpHeaders.EXPIRES.toLowerCase())) {
      expiry = extractV2ExpiryDate(args, effectiveTimeZone);
    } else if (args.containsKey(SignerConstants.X_AMZ_EXPIRES.toLowerCase())
        && (args.containsKey(SignerConstants.X_AMZ_DATE.toLowerCase()))) {
      expiry = extractV4ExpiryDate(args, effectiveTimeZone);
    } else {
      // Missing expected query arguments
      log.error("Could not identify expected date parameters in request: {}", flattenMap(args));
      throw new IllegalArgumentException("Could not parse presigned URL - missing expected expiry date parameters");
    }
    log.debug("Expiry DateTime (Local): {}\n", expiry);
    return expiry;
  }

  /**
   * V2 Signature uses 'Expires' HTTP Header
   * @param args
   * @param effectiveTimeZone
   * @return timestamp from header
   */
  LocalDateTime extractV2ExpiryDate(Map<String, String> args, ZoneId effectiveTimeZone) {
    val ts = args.get(HttpHeaders.EXPIRES.toLowerCase());
    val explicit = Instant.ofEpochMilli(Long.parseLong(ts) * 1000);
    return LocalDateTime.ofInstant(explicit, effectiveTimeZone);
  }

  /**
   * V4 Signature uses 'X-Amz-Expires' and 'X-Amz-Date' headers
   * @param args
   * @param effectiveTimeZone
   * @return timestamp from header
   */
  LocalDateTime extractV4ExpiryDate(Map<String, String> args, ZoneId effectiveTimeZone) {
    String ts = "";
    try {
      ts = args.get(SignerConstants.X_AMZ_DATE.toLowerCase()); // Basic format ISO 8601 string in UTC
      val reqDate = ZonedDateTime.parse(ts, formatter);
      log.trace("Request DateTime (Zoned): {}", reqDate);

      val expSeconds = Integer.parseInt(args.get(SignerConstants.X_AMZ_EXPIRES.toLowerCase()));

      // Translate to effective timezone (and increment with expiry period)
      return reqDate.withZoneSameInstant(effectiveTimeZone).toLocalDateTime().plusSeconds(expSeconds);
    } catch (DateTimeParseException pe) {
      log.error("{} is not parsable!", ts);
      throw pe; // Rethrow the exception.
    }
  }
}
