/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import java.io.File;

import lombok.SneakyThrows;
import lombok.val;

import com.google.common.collect.ImmutableList;

/**
 * Spring boot process wrapper.
 */
public class SpringBootProcess {

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

    val process = new ProcessBuilder(args).inheritIO().start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroyForcibly()));
    return process;
  }

}
