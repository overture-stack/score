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
package bio.overture.score.client.ssl;

import bio.overture.score.client.config.ClientProperties;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

import static com.google.common.base.Preconditions.checkState;

/**
 * a tool to generate client side certificate
 */
@Slf4j
public class ClientKeyTool {

  private static final String SHA256_WITH_RSA_ENCRYPTION = "SHA256WithRSAEncryption";
  private final static X500Name x500Name = new X500NameBuilder(BCStyle.INSTANCE)
      .addRDN(BCStyle.OU, "Software Development")
      .addRDN(BCStyle.O, "OICR")
      .addRDN(BCStyle.CN, "www.collaboratory.org")
      .addRDN(BCStyle.C, "CA")
      .addRDN(BCStyle.L, "Ontario").build();
  private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

  private final ClientProperties.SSLProperties ssl;
  private SecureRandom sr = new SecureRandom();

  public ClientKeyTool(ClientProperties.SSLProperties ssl) {
    Security.addProvider(new BouncyCastleProvider());
    this.ssl = ssl;
  }

  @SneakyThrows
  public Certificate createIfAbsent() throws KeyStoreException {
    log.info("Inside createIfAbsent method");
    KeyStore ks = loadStore();
    log.info("Is Key store null? : " + (ks == null ? "true" : "false"));
    Certificate cert = findCertifcate(ks);
    if (!ssl.getKeyStore().exists() || cert == null) {
      log.info("Either the certificate or keystore does not exist");
      val keystoreDir = ssl.getKeyStore().getFile().getParentFile();
      checkState(keystoreDir.mkdirs());

      val keyPair = createKeyPair();
      cert = createCertificate(keyPair);
      ks.setKeyEntry(ssl.getKeyAlias(), keyPair.getPrivate(),
          ssl.getKeyStorePassword().toCharArray(), new Certificate[] { cert });
    }
    saveStore(ks);
    return cert;
  }

  @SneakyThrows
  private Certificate findCertifcate(KeyStore keyStore) {
    log.info("Inside the findCertificate method...");
    Certificate cert = null;
    if (keyStore.containsAlias(ssl.getKeyAlias())) {
      // validate the certificate if it is still valid
      cert = keyStore.getCertificate(ssl.getKeyAlias());
      if (cert instanceof X509Certificate) {
        try {
          ((X509Certificate) cert).checkValidity();
        } catch (CertificateExpiredException | CertificateNotYetValidException e) {
          keyStore.deleteEntry(ssl.getKeyAlias());
        }
      }
    }
    return cert;
  }

  @SneakyThrows
  private KeyPair createKeyPair() {
    log.info("Inside the createKeyPair method..");
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    keyGen.initialize(2048, sr);
    KeyPair keyPair = keyGen.generateKeyPair();
    return keyPair;
  }

  @SneakyThrows
  private void saveStore(KeyStore ks) {
    log.info("Entered the saveStore method");
    @Cleanup
    FileOutputStream fos = new FileOutputStream(ssl.getKeyStore().getFile());
    ks.store(fos, ssl.getKeyStorePassword().toCharArray());
  }

  @SneakyThrows
  private KeyStore loadStore() {
    log.info("Entered the loadStore Method");
    KeyStore ks = KeyStore.getInstance(ssl.getKeyStoreType());
    log.info("Is Key store null? : " + (ks == null ? "true" : "false"));
    char[] password = ssl.getKeyStorePassword().toCharArray();
    if (ssl.getKeyStore().getFile().exists()) {
      ks.load(ssl.getKeyStore().getInputStream(), password);
    } else {
      ks.load(null, password);
    }
    return ks;
  }

  @SneakyThrows
  private Certificate createCertificate(KeyPair keyPair) {
    log.info("Entered the createCertificate Method...");
    LocalDate today = LocalDate.now();
    X509v3CertificateBuilder certGen =
        new JcaX509v3CertificateBuilder(x500Name,
            BigInteger.valueOf(sr.nextInt(Integer.MAX_VALUE)), today.minusDays(1).toDate(),
            today.plusYears(3).toDate(), x500Name, keyPair.getPublic());
    ContentSigner sigGen = new JcaContentSignerBuilder(SHA256_WITH_RSA_ENCRYPTION)
        .setProvider(BC).build(keyPair.getPrivate());
    X509Certificate cert = new JcaX509CertificateConverter().setProvider(BC)
        .getCertificate(certGen.build(sigGen));
    return cert;
  }

  @SneakyThrows
  public void export(X509Certificate cert, File outputFile) {
    log.info("Entered the export method...");
    @Cleanup
    FileOutputStream fos = new FileOutputStream(outputFile);
    fos.write(cert.getEncoded());
  }
}
