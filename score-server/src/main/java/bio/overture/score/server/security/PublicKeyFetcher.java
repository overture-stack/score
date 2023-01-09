package bio.overture.score.server.security;

@FunctionalInterface
public interface PublicKeyFetcher {

  String getPublicKey();
}
