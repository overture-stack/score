package bio.overture.score.client.storage;

import bio.overture.score.core.model.DataChannel;
import bio.overture.score.core.model.ObjectInfo;
import bio.overture.score.core.model.ObjectSpecification;
import bio.overture.score.core.model.Part;
import bio.overture.score.core.model.UploadProgress;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface StorageService {

  List<ObjectInfo> listObjects();

  UploadProgress getProgress(String objectId, long fileSize) throws IOException;

  void downloadPart(DataChannel channel, Part part, String objectId, File outputDir)
      throws IOException;

  void uploadPart(DataChannel channel, Part part, String objectId, String uploadId)
      throws IOException;

  ObjectSpecification initiateUpload(String objectId, long length, boolean overwrite, String md5)
      throws IOException;

  void finalizeDownload(File outDir, String objectId) throws IOException;

  void finalizeUpload(String objectId, String uploadId) throws IOException;

  void finalizeUploadPart(
      String objectId,
      String uploadId,
      int partNumber,
      String md5,
      String etag,
      boolean disableChecksum)
      throws IOException;

  boolean isObjectExist(String objectId) throws IOException;

  ObjectSpecification getDownloadSpecification(String objectId, long offset, long length)
      throws IOException;

  ObjectSpecification getExternalDownloadSpecification(String objectId, long offset, long length)
      throws IOException;

  void deleteDownloadPart(File stateDir, String objectId, Part part);

  void deleteUploadPart(String objectId, String uploadId, Part part) throws IOException;

  boolean isDownloadDataRecoverable(File stateDir, String objectId, long fileSize)
      throws IOException;

  boolean isUploadDataRecoverable(String objectId, long fileSize) throws IOException;

  String ping();

  default ObjectSpecification getDownloadSpecification(String objectId) throws IOException {
    return getDownloadSpecification(objectId, 0L, -1L);
  }
}
