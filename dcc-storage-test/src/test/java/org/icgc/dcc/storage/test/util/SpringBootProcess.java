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

import com.google.common.collect.ImmutableList;

import lombok.SneakyThrows;
import lombok.val;

/**
 * Spring boot process wrapper.
 */
public class SpringBootProcess {

  public static Process bootRun(Class<?> mainClass, String... config) {
    return bootRun(mainClass, new String[] {}, config);
  }

  public static Process bootRun(Class<?> mainClass, String[] args, String... config) {
    val jarFile = new File(mainClass.getProtectionDomain().getCodeSource().getLocation().getPath());
    return bootRun(jarFile, args, config);
  }

  public static Process bootRun(File jarFile, String... config) {
    return bootRun(jarFile, new String[] {}, config);
  }

  @SneakyThrows
  public static Process bootRun(File jarFile, String[] args, String... config) {
    args = ImmutableList.<String> builder()
        .add("java", "-Ds3ninja=true", "-jar", jarFile.getCanonicalPath())
        .add(args)
        .add(config).build()
        .toArray(new String[args.length + 1]);

    val process = new ProcessBuilder(args).inheritIO().start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroyForcibly()));
    return process;
  }

}
