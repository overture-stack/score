package bio.overture.score.server.security.ssl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class DumbHostnameVerifier implements HostnameVerifier {
  public DumbHostnameVerifier() {}

  public boolean verify(String hostname, SSLSession sslSession) {
    return true;
  }
}
