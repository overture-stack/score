package collaboratory.storage.object.store.client.cli.command;

import java.io.File;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import collaboratory.storage.object.store.client.upload.ObjectUpload;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Component
@Parameters(separators = "=", commandDescription = "file upload")
@Slf4j
public class UploadCommand extends AbstractClientCommand {

  @Parameter(names = "--file", description = "Path to a file", required = true)
  private String filePath;

  @Parameter(names = "-f", description = "force to re-upload", required = false)
  private boolean isForce = false;

  @Parameter(names = "--object-id", description = "object id assigned to the file", required = true)
  private String oid;

  @Autowired
  private ObjectUpload uploadService;

  @Override
  @SneakyThrows
  public int execute() {

    println("Start uploading file: %s", filePath);
    log.info("file: {}", filePath);
    File upload = new File(filePath);
    uploadService.upload(upload, oid, isForce);
    return SUCCESS_STATUS;
  }

}
