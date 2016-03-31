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
package org.icgc.dcc.storage.client.ssl;

import javax.net.ssl.SSLException;

import org.apache.http.conn.ssl.AbstractVerifier;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Custom verifier for cases where the client locally has the server's key installed in the trust store.
 * <p>
 * Rationale is that the server is trusted by key instead.
 * 
 * TODO: Port to non-deprecated abstractions.
 */
@RequiredArgsConstructor
public final class ExcludingHostnameVerifier extends AbstractVerifier {

  /**
   * Configuration.
   */
  @NonNull
  private final String excludedCn;

  @Override
  public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
    // Short circuit certificate checking if cn is implicitly trusted
    if (isExcluded(cns)) {
      // Return indicates success
      return;
    }

    // Verify valid certificates as per usual
    verify(host, cns, subjectAlts, true);
  }

  private boolean isExcluded(String[] cns) {
    for (val cn : cns) {
      val exactMatch = cn.equals(excludedCn);
      if (exactMatch) {
        return true;
      }
    }

    return false;
  }

}