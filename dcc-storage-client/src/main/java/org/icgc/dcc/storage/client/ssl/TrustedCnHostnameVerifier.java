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

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Custom verifier for cases where the client locally has the server's key installed in the trust store with a CN having
 * the same value as {@code trustedCn}.
 * <p>
 * Rationale is that the server is trusted by trusted key instead.
 * 
 * TODO: Consider doing something like:
 * https://github.com/getoutreach/outreach-platform-sdk/blob/bf1e4a5c90d95a3bca8a6b1204b03c83f8bbf898/java/src/main/java
 * /io/outreach/security/TrustedHostnameVerifier.java
 */
public final class TrustedCnHostnameVerifier implements HostnameVerifier {

  /**
   * Configuration.
   */
  private final String trustedCn;
  private final HostnameVerifier delegate;

  public TrustedCnHostnameVerifier(String excludedCn, @NonNull HostnameVerifier delegate) {
    this.trustedCn = excludedCn;
    this.delegate = delegate;
  }

  public TrustedCnHostnameVerifier(@NonNull String trustedCn) {
    this(trustedCn, new DefaultHostnameVerifier());
  }

  @Override
  public boolean verify(String host, SSLSession session) {
    val cert = getCert(session);

    // If trusted, don't verify certificate
    if (isTrusted(cert)) {
      return true;
    }

    // Verify valid certificate as per usual
    return delegate.verify(host, session);
  }

  private boolean isTrusted(X509Certificate cert) {
    val cn = getCN(cert);
    val exactMatch = cn.equals(trustedCn);

    return exactMatch;
  }

  @SneakyThrows
  private static X509Certificate getCert(SSLSession session) {
    final Certificate[] certs = session.getPeerCertificates();

    return (X509Certificate) certs[0];
  }

  @SneakyThrows
  private static String getCN(X509Certificate cert) {
    // See http://stackoverflow.com/questions/2914521/how-to-extract-cn-from-x509certificate-in-java#5527171
    val x500name = new JcaX509CertificateHolder(cert).getSubject();
    val cn = x500name.getRDNs(BCStyle.CN)[0];

    return IETFUtils.valueToString(cn.getFirst().getValue());
  }

}