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

public class BootProcess {

  @SneakyThrows
  public static Process bootProcess(Class<?> mainClass, String... args) {
    val jarFile = new File(mainClass.getProtectionDomain().getCodeSource().getLocation().getPath());
    return bootProcess(jarFile, args);
  }

  @SneakyThrows
  public static Process bootProcess(File jarFile, String... args) {
    args = ImmutableList.<String> builder()
        .add("java", "-jar", jarFile.getCanonicalPath())
        .add(args).build()
        .toArray(new String[args.length + 1]);

    val process = new ProcessBuilder(args).inheritIO().start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> process.destroyForcibly()));
    return process;
  }

}
