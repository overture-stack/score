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
package org.icgc.dcc.storage.client.command;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.icgc.dcc.storage.client.cli.Parameters.checkParameter;
import static org.icgc.dcc.storage.client.mount.MountService.INTERNAL_MOUNT_OPTIONS;
import static org.icgc.dcc.storage.client.util.Formats.formatBytes;
import static org.icgc.dcc.storage.client.util.Formats.formatBytesUnits;
import static org.icgc.dcc.storage.fs.util.Formats.formatCount;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.storage.client.cli.ConverterFactory.MountOptionsConverter;
import org.icgc.dcc.storage.client.cli.ConverterFactory.StorageFileLayoutConverter;
import org.icgc.dcc.storage.client.cli.DirectoryValidator;
import org.icgc.dcc.storage.client.download.DownloadService;
import org.icgc.dcc.storage.client.manifest.ManifestResource;
import org.icgc.dcc.storage.client.manifest.ManifestService;
import org.icgc.dcc.storage.client.metadata.Entity;
import org.icgc.dcc.storage.client.metadata.MetadataService;
import org.icgc.dcc.storage.client.mount.MountService;
import org.icgc.dcc.storage.client.mount.MountStorageContext;
import org.icgc.dcc.storage.client.transport.StorageService;
import org.icgc.dcc.storage.core.model.ObjectInfo;
import org.icgc.dcc.storage.fs.StorageFileLayout;
import org.icgc.dcc.storage.fs.StorageFileSystems;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.sun.akuma.Daemon;
import com.sun.akuma.JavaVMArguments;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Slf4j
@Component
@Parameters(separators = "=", commandDescription = "Mount a read-only FUSE file system view of the remote storage repository")
public class MountCommand extends RepositoryAccessCommand {

  /**
   * Constants.
   */
  private static final String FUSE_README_URL = "http://sourceforge.net/p/fuse/fuse/ci/master/tree/README?format=raw";

  /**
   * Options
   */
  @Parameter(names = "--mount-point", description = "The mount point of the FUSE file system. This must exist, be empty and be executable by the current user.", required = true, validateValueWith = DirectoryValidator.class)
  private File mountPoint;
  @Parameter(names = "--manifest", description = "Manifest id (from the Data Portal), url or file path")
  private ManifestResource manifestResource;
  @Parameter(names = "--layout", description = "Layout of the mount point. One of 'bundle' (nest files in bundle directory) or 'object-id' (flat list of files named by their associated object id)", converter = StorageFileLayoutConverter.class)
  private StorageFileLayout layout = StorageFileLayout.BUNDLE;
  @Parameter(names = "--cache-metadata", description = "To speedup load times, cache metadata on disk locally and use if available")
  private boolean cacheMetadata;
  @Parameter(names = "--daemonize", description = "Detach and run in background")
  private boolean daemonize;
  @Parameter(names = "--verify-connection", description = "Verify connection to repository", arity = 1)
  private boolean verifyConnection = true;
  @Parameter(names = "--options", description = "The mount options of the file system (e.g. --options user_allow_other,allow_other,fsname=icgc,debug) "
      + "in addition to those specified internally: " + INTERNAL_MOUNT_OPTIONS + ". See " + FUSE_README_URL
      + " for details", converter = MountOptionsConverter.class)
  private Map<String, String> options = newHashMap();

  /**
   * Dependencies.
   */
  @Autowired
  private ManifestService manifestService;
  @Autowired
  private MetadataService metadataServices;
  @Autowired
  private StorageService storageService;
  @Autowired
  private DownloadService downloadService;
  @Autowired
  private MountService mountService;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Override
  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "JCommander parameter ensures File is valid")
  public int execute() throws Exception {
    checkParameter(mountPoint.canExecute(),
        "Cannot mount to '%s'. Please check directory permissions and try again", mountPoint);
    checkParameter(mountPoint.list() != null && mountPoint.list().length == 0,
        "Cannot mount to '%s'. Please ensure the directory is empty and is not already mounted", mountPoint);

    // If requested, put into the background
    if (daemonize()) {
      return SUCCESS_STATUS;
    }

    if (verifyConnection) {
      try {
        verifyRepoConnection();
      } catch (IOException ioe) {
        terminal.printError("Could not verify connection to Repository. " + ioe.getMessage());
      }
    }

    try {
      int i = 1;

      //
      // Collect and index metadata
      //

      val tip =
          cacheMetadata ? "" : " (Tip: use " + terminal.option("--cache-metadata") + " to skip this step next time)";

      log.info("Indexing remote entities...");
      terminal.printStatus(i++, "Indexing remote entities" + tip + ". Please wait");
      val entities = terminal.printWaiting(this::resolveEntities);

      log.info("Indexing remove objects...");
      terminal.printStatus(i++, "Indexing remote objects" + tip + ". Please wait");
      List<ObjectInfo> objects = terminal.printWaiting(this::resolveObjects);
      if (hasManifest()) {
        // Manifest is a filtered view y'all!
        objects = filterManifestObjects(objects);
      }

      //
      // Check access
      //

      log.info("Checking access...");
      terminal.printStatus(i++, "Checking access. Please wait");
      val context =
          new MountStorageContext(layout, downloadService, entities, objects);
      if (!terminal.printWaiting(context::isAuthorized)) {
        terminal.printError("Access denied");
        return FAILURE_STATUS;
      }

      //
      // Report manifest
      //

      if (hasManifest()) {
        log.info("Applying manifest view...");
        terminal.printStatus(i++, "Applying manifest view:\n");
        reportManifest(context);
      }

      //
      // Mount
      //

      terminal.printStatus(i++, "Mounting file system to '" + mountPoint.getAbsolutePath() + "'");
      terminal.printWaiting(() -> mount(context));
      reportMount();

      //
      // Wait
      //

      // Let the user know we are done when the JVM exits
      val watch = Stopwatch.createStarted();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        reportSummary(context, watch);
      }));

      // Wait for interrupt
      Thread.sleep(Long.MAX_VALUE);
    } catch (InterruptedException ie) {
      return SUCCESS_STATUS;
    } catch (Exception e) {
      log.error("Unknown error:", e);
      throw e;
    }

    return SUCCESS_STATUS;
  }

  private boolean daemonize() throws Exception, IOException {
    val daemon = new Daemon();
    if (daemon.isDaemonized()) {
      daemon.init();
    } else {
      if (daemonize) {
        val args = JavaVMArguments.current();
        for (int i = 0; i < args.size(); i++) {
          val arg = args.get(i);
          if (arg.equals("mount")) {
            args.add(i, "--silent");
            break;
          }
        }

        terminal.printStatus(terminal.value("Daemonizing...")).println("");
        daemon.daemonize(args);
        return true;
      }
    }

    return false;
  }

  @SneakyThrows
  private void mount(MountStorageContext context) {
    val fileSystem = StorageFileSystems.newFileSystem(context);
    mountService.mount(fileSystem, mountPoint.toPath(), options);
  }

  //
  // Reporting
  //

  private void reportManifest(MountStorageContext context) {
    terminal.printLine();
    terminal.println(terminal.ansi("@|bold <object id>: <gnos id>/<file name> @ <file size>|@"));
    terminal.printLine();

    long totalSize = 0;
    val files = context.getFiles();
    log.info("Files: {}", files);

    for (val file : files) {
      terminal.printf(" - %s: %s/%s %s %s %s%n",
          terminal.ansi("@|blue " + file.getObjectId() + "|@"),
          terminal.ansi("@|green " + file.getGnosId() + "|@"),
          terminal.ansi("@|green " + file.getFileName() + "|@"),
          terminal.ansi("@|bold @|@"),
          formatBytes(file.getSize()),
          terminal.ansi("@|bold " + formatBytesUnits(file.getSize()) + "|@"));

      totalSize += file.getSize();
    }

    terminal.printLine();
    terminal.println(" Total count: " + formatCount(files.size()) +
        ", Total size: " + formatBytes(totalSize) + " " + formatBytesUnits(totalSize) + "\n");
  }

  private void reportMount() {
    val location = terminal.value(mountPoint.getAbsolutePath());
    terminal.printStatus(
        terminal.label("Successfully mounted file system at " + location + " and is now ready for use."));

    terminal.print("\nOpen a new terminal for interaction or relaunch with " + terminal.option("--daemonize")
        + " to put in background");
  }

  private void reportSummary(MountStorageContext context, Stopwatch watch) {
    val c = firstNonNull(context.getMetrics().get("connectCount"), 0L);
    val n = firstNonNull(context.getMetrics().get("byteCount"), 0L);

    val time = terminal.value(watch.toString());
    val connects = terminal.value(c + " connects");
    val bytes = terminal.value(formatBytes(n) + " " + formatBytesUnits(n));
    val status = "Shut down mount after " + time + " with a total of " + connects + " and " + bytes + " bytes read.\n";

    terminal.printStatus(terminal.label(status));
  }

  //
  // Resolving
  //

  private List<ObjectInfo> resolveObjects() throws IOException {
    return resolveList("objects", storageService::listObjects, new TypeReference<List<ObjectInfo>>() {});
  }

  private List<Entity> resolveEntities() throws IOException {
    return resolveList("entities", () -> metadataServices.getEntities("id", "fileName", "gnosId"),
        new TypeReference<List<Entity>>() {});
  }

  @SneakyThrows
  private <T> List<T> resolveList(String name, Supplier<List<T>> factory, TypeReference<List<T>> typeReference) {
    val cacheFile = new File("." + name + ".cache");
    if (cacheMetadata && cacheFile.exists()) {
      return MAPPER.readValue(cacheFile, typeReference);
    }

    val values = factory.get();
    if (cacheMetadata) {
      MAPPER.writeValue(cacheFile, values);
    }

    return values;
  }

  //
  // Utilities
  //

  private boolean hasManifest() {
    return manifestResource != null;
  }

  private List<ObjectInfo> filterManifestObjects(List<ObjectInfo> objects) {
    val manifest = manifestService.getDownloadManifest(manifestResource);

    validateManifest(manifest);

    val objectIds = manifest.getEntries().stream()
        .flatMap(entry -> Stream.of(entry.getFileUuid(), entry.getIndexFileUuid()))
        .collect(toSet());

    return objects.stream()
        .filter(object -> objectIds.contains(object.getId()))
        .collect(toList());
  }

}
