package bio.overture.score.client.transport;

import bio.overture.score.core.model.DataChannel;
import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.core.model.Part;

import java.io.File;
import java.io.IOException;

public interface StorageDownloadService {
  void downloadPart(DataChannel channel, Part part, String objectId, File outputDir) throws IOException;
  void finalizeDownload(File outDir, String objectId) throws IOException;
  ObjectSpecification getExternalDownloadSpecification(String objectId, long offset, long length) throws IOException;
  ObjectSpecification getDownloadSpecification(String objectId, long offset, long length) throws IOException;
  void deleteDownloadPart(File stateDir, String objectId, Part part) throws IOException;
  boolean isDownloadDataRecoverable(File stateDir, String objectId, long fileSize) throws IOException;

  default ObjectSpecification getDownloadSpecification(String objectId) throws IOException{
    return getDownloadSpecification(objectId, 0L, -1L);
  }

}
