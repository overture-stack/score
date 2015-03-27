package collaboratory.storage.object.store.core.factory;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import lombok.SneakyThrows;


import sun.security.util.HostnameChecker;

import collaboratory.storage.object.store.core.util.DumbX509TrustManager;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

public final class JerseyClientFactory {

  @SneakyThrows
  public static Client getDefaultInstance(boolean isStrict) {
    return Client.create(getClientConfig(isStrict, null, null, null));
  }

  @SneakyThrows
  public static Client getDefaultInstance(boolean isStrict, String trustName, TrustManagerFactory tmf,
      KeyManagerFactory kmf) {
    return Client.create(getClientConfig(isStrict, trustName, tmf, kmf));
  }

  @SneakyThrows
  public static Client createJsonResponseInstance(final boolean isStrict) {
    ClientConfig cc = getClientConfig(isStrict, null, null, null);
    cc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    cc.getClasses().add(JacksonJsonProvider.class);
    return Client.create(cc);
  }

  private static ClientConfig getClientConfig(final boolean isStrict, final String trustName,
      final TrustManagerFactory tmf, final KeyManagerFactory kmf)
      throws NoSuchAlgorithmException,
      KeyManagementException {
    ClientConfig config = new DefaultClientConfig();
    if (!isStrict) {

      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, new TrustManager[] { new DumbX509TrustManager() }, null);

      config.getProperties().put(
          HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
          new HTTPSProperties(new HostnameVerifier() {

            @Override
            public boolean verify(String hostname,
                SSLSession sslSession) {
              return true;
            }

          }, context));
    } else if (trustName != null) {
      try {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        final HostnameChecker checker = HostnameChecker.getInstance(HostnameChecker.TYPE_TLS);
        config.getProperties().put(
            HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
            new HTTPSProperties(new HostnameVerifier() {

              @Override
              public boolean verify(String hostname,
                  SSLSession session) {
                try {
                  Certificate[] peerCertificates = session.getPeerCertificates();
                  if (peerCertificates.length > 0) {
                    X509Certificate peerCertificate = (X509Certificate) peerCertificates[0];
                    checker.match(trustName, peerCertificate); // this can throw exceptions
                    return true;
                  }
                } catch (SSLPeerUnverifiedException |
                    CertificateException e) {
                  throw new RuntimeException(e);
                }
                return false;
              }

            }, context));
      } catch (NoSuchAlgorithmException | KeyManagementException e) {
        throw new RuntimeException("Unable to create a HTTPS client", e);
      }
    }
    return config;
  }
}
