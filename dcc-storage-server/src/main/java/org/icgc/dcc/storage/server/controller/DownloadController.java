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
package org.icgc.dcc.storage.server.controller;

import javax.servlet.http.HttpServletRequest;

import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.server.repository.DownloadService;
import org.icgc.dcc.storage.server.security.TokenHasher;
import org.icgc.dcc.storage.server.util.HttpServletRequests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A controller to expose RESTful API for download
 */
@Setter
@RestController
@RequestMapping("/download")
@Slf4j
@Profile({ "prod", "default", "debug" })
public class DownloadController {

  @Autowired
  DownloadService downloadService;

  @RequestMapping(method = RequestMethod.GET, value = "/ping")
  public @ResponseBody String ping(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String accessToken,
      @RequestHeader(value = "User-Agent", defaultValue = "unknown") String userAgent, HttpServletRequest request) {

    val ipAddress = HttpServletRequests.getIpAddress(request);

    log.info("Requesting download of sentinel object id with access token {} (MD5) from {} and client version {}",
        identifier(accessToken), ipAddress, userAgent);
    return downloadService.getSentinelObject();
  }

  @PreAuthorize("@accessSecurity.authorize(authentication,#objectId)")
  @RequestMapping(method = RequestMethod.GET, value = "/{object-id}")
  public @ResponseBody ObjectSpecification downloadPartialObject(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) final String accessToken,
      @PathVariable(value = "object-id") String objectId,
      @RequestParam(value = "offset", required = true) long offset,
      @RequestParam(value = "length", required = true) long length,
      @RequestParam(value = "external", defaultValue = "false") boolean external,
      @RequestHeader(value = "User-Agent", defaultValue = "unknown") String userAgent,
      HttpServletRequest request) {

    val ipAddress = HttpServletRequests.getIpAddress(request);

    log.info("Requesting download of object id {} with access token {} (MD5) from {} and client version {}", objectId,
        identifier(accessToken), ipAddress, userAgent);
    return downloadService.download(objectId, offset, length, external);
  }

  protected String identifier(String accessToken) {
    String identifier = "<none>";
    if ((accessToken != null) && (!accessToken.isEmpty())) {
      identifier = TokenHasher.hashToken(accessToken);
    }
    return identifier;
  }

  /**
   * Exception handler specific to the Spring Security processing in this controller
   * @return Error if Spring Security policies are violated
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> handleAccessDeniedException(HttpServletRequest req, AccessDeniedException ex) {
    log.error("Token missing required scope to download controlled-access file");
    return new ResponseEntity<Object>("Token missing required scope to download controlled-access file",
        new HttpHeaders(),
        HttpStatus.FORBIDDEN);
  }

}
