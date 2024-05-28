package bio.overture.score.client.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class SSLCertificateValidation {
  public static void disable() {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      TrustManager[] trustManagerArray = new TrustManager[] {new DumbX509TrustManager()};
      sslContext.init((KeyManager[]) null, trustManagerArray, (SecureRandom) null);
      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier(new DumbHostnameVerifier());
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      throw new RuntimeException(e);
    }
  }

  private SSLCertificateValidation() {}
}
