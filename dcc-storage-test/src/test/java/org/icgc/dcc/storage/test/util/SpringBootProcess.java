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
package org.icgc.dcc.storage.test.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.StandardSystemProperty.JAVA_CLASS_PATH;

import java.io.File;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.common.core.util.Splitters;

import com.google.common.collect.ImmutableList;

/**
 * Spring boot process wrapper.
 */
@Slf4j
public class SpringBootProcess {

  public static Process bootRun(String artifactId, int debugPort, String[] args, String... systemProperties) {
    val file = findArtifact(artifactId);
    return bootRun(file, debugPort, args, systemProperties);
  }

  public static Process bootRun(String artifactId, int debugPort, String... systemProperties) {
    return bootRun(artifactId, debugPort, new String[] {}, systemProperties);
  }

  public static Process bootRun(Class<?> mainClass, int debugPort, String... systemProperties) {
    return bootRun(mainClass, debugPort, new String[] {}, systemProperties);
  }

  public static Process bootRun(Class<?> mainClass, int debugPort, String[] args, String... systemProperties) {
    val jarFile = new File(mainClass.getProtectionDomain().getCodeSource().getLocation().getPath());
    return bootRun(jarFile, debugPort, args, systemProperties);
  }

  public static Process bootRun(File jarFile, int debugPort, String... systemProperties) {
    return bootRun(jarFile, debugPort, new String[] {}, systemProperties);
  }

  /**
   * 
   * @param debugPort - specifying a debug port will cause the process to suspend until you attach a remote debugging
   * session to that port. Typically this is set as a java system property: -Dstorage.server.debugPort=8000
   */
  @SneakyThrows
  public static Process bootRun(File jarFile, int debugPort, String[] args, String... systemProperties) {
    ImmutableList.Builder<String> bob = ImmutableList.<String> builder().add("java");

    if (debugPort > 0) {
      bob.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=" + debugPort);
    }
    args = bob.add(systemProperties)
        .add("-Ds3ninja=true", "-jar", jarFile.getCanonicalPath())
        .add(args).build()
        .toArray(new String[args.length + 1]);
    log.info(Joiners.WHITESPACE.join(args));
    val process = new ProcessBuilder(args).inheritIO().start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroyForcibly()));
    return process;
  }

  @SneakyThrows
  private static File findArtifact(String artifactId) {
    // Try other dependencies
    val paths = Splitters.COLON.splitToList(System.getProperty(JAVA_CLASS_PATH.key()));
    for (val path : paths) {
      val file = new File(path);
      if (file.getName().startsWith(artifactId)) {
        return file;
      }
    }

    // Try project dependencies
    val targetDir = new File("../" + artifactId + "/target").getCanonicalFile();
    File[] localFiles =
        targetDir.listFiles((File file, String name) -> name.startsWith(artifactId) && name.endsWith(".jar"));
    if (localFiles != null && localFiles.length > 0) {
      return localFiles[0];
    }

    checkArgument(false, "Could not find artifact %s in %s or %s", artifactId, targetDir, paths);

    return null;
  }

}
