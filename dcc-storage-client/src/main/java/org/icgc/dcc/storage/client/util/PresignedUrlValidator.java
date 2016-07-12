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

import java.net.URL;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

@Slf4j
public class PresignedUrlValidator {

  @SneakyThrows
  public static boolean isUrlExpired(String urlString) {
    val url = new URL(urlString);
    return isUrlExpired(url);
  }

  public static boolean isUrlExpired(URL url) {
    val expiry = getExpiry(url);
    return isExpired(expiry);
  }

  @SneakyThrows
  public static LocalDateTime getExpiry(String urlString) {
    val url = new URL(urlString);
    return getExpiry(url);
  }

  public static LocalDateTime getExpiry(URL url) {
    val bits = URLEncodedUtils.parse(url.getQuery(), Charset.defaultCharset());
    val args = collectQuery(bits);

    return extractExpiryDate(args);
  }

  /**
   * Uses system default TimeZone for expiry date calculations
   * @param args
   * @return
   */
  static LocalDateTime extractExpiryDate(Map<String, String> args) {
    return extractExpiryDate(args, ZoneId.systemDefault());
  }

  /**
   * Return expiry date of presigned URL in the local time zone (system default). Distinguished between V2 and V4 AWS
   * signatures
   * @param args - URL query arguments
   * @return expiry date translated to specified time zone
   */
  static LocalDateTime extractExpiryDate(Map<String, String> args, ZoneId effectiveTimeZone) {
    LocalDateTime expiry = null;
    if (args.containsKey("Expires".toLowerCase())) {
      val ts = args.get("Expires".toLowerCase());
      val explicit = Instant.ofEpochMilli(Long.parseLong(ts) * 1000);
      expiry = LocalDateTime.ofInstant(explicit, effectiveTimeZone);
    } else if (args.containsKey("X-Amz-Expires".toLowerCase()) && (args.containsKey("X-Amz-Date".toLowerCase()))) {
      String ts = "";
      try {
        // using string literals for query parameter names because the values found here:
        // http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/constant-values.html
        // don't appear consistent: all lower case, no X-Amz-Expired constant at all.

        ts = args.get("X-Amz-Date".toLowerCase()); // basic format ISO 8601 string in UTC
        // extended format uses hyphens. Named formatters in Java 8 (like ISO_INSTANT) are extended format only
        // So, we have to define formatter with explicit ISO 8601 basic format
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");
        val reqDate = ZonedDateTime.parse(ts, formatter);
        log.trace("Request DateTime (Zoned): %s%n", reqDate);

        val expSeconds = Integer.parseInt(args.get("X-Amz-Expires".toLowerCase()));

        // translate to effective timezone (and increment with expiry period)
        expiry = reqDate.withZoneSameInstant(effectiveTimeZone).toLocalDateTime().plusSeconds(expSeconds);
        System.out.printf("Expiry DateTime (Local): %s%n", expiry);

      } catch (DateTimeParseException pe) {
        log.error("%s is not parsable!%n", ts);
        throw pe; // Rethrow the exception.
      }
    }
    return expiry;
  }

  static boolean isExpired(LocalDateTime expiry) {
    val now = LocalDateTime.now();
    log.trace("Now DateTime: %s%n", now);
    log.trace("Specified Expiry: %s%n", expiry);

    // TOOD: remove this bit
    if (expiry.isBefore(now)) {
      System.out.println("Expired!");
    } else {
      System.out.println("Not expired; still " + ChronoUnit.SECONDS.between(now, expiry) + " seconds to go");
    }
    return expiry.isBefore(now);
  }

  static Map<String, String> collectQuery(List<NameValuePair> args) {
    val result = new HashMap<String, String>();
    for (val pair : args) {
      // store keys as lower case by convention
      // System.out.println(pair.getName().toLowerCase() + " = " + pair.getValue());
      result.put(pair.getName().toLowerCase(), pair.getValue());
    }
    return result;
  }
}
