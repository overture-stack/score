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

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.google.common.collect.ImmutableMap;

@Slf4j
public abstract class PresignedUrlValidator {

  @SneakyThrows
  public boolean isUrlExpired(String urlString) {
    val url = new URL(urlString);
    return isUrlExpired(url);
  }

  public boolean isUrlExpired(URL url) {
    val expiry = getExpiry(url);
    return isExpired(expiry);
  }

  @SneakyThrows
  public LocalDateTime getExpiry(String urlString) {
    val url = new URL(urlString);
    return getExpiry(url);
  }

  public LocalDateTime getExpiry(URL url) {
    val bits = URLEncodedUtils.parse(url.getQuery(), Charset.defaultCharset());
    val args = collectQuery(bits);

    return extractExpiryDate(args);
  }

  /**
   * Uses system default TimeZone for expiry date calculations
   * @param args
   * @return
   */
  LocalDateTime extractExpiryDate(Map<String, String> args) {
    return extractExpiryDate(args, ZoneId.systemDefault());
  }

  abstract LocalDateTime extractExpiryDate(Map<String, String> args, ZoneId effectiveTimeZone);

  boolean isExpired(LocalDateTime expiry) {
    val now = LocalDateTime.now();
    log.trace("Now DateTime: %s%n", now);
    log.trace("Specified Expiry: %s%n", expiry);

    return expiry.isBefore(now);
  }

  Map<String, String> collectQuery(List<NameValuePair> args) {
    val result = new ImmutableMap.Builder<String, String>();
    for (val pair : args) {
      // Store keys as lower case by convention
      result.put(pair.getName().toLowerCase(), pair.getValue());
    }
    return result.build();
  }

  String flattenMap(Map<String, String> contents) {
    String buffer = "";
    for (val entry : contents.entrySet()) {
      buffer += (entry.toString() + " ");
    }
    return buffer;
  }

}
