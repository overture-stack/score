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
package org.icgc.dcc.score.test.util;

import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import lombok.SneakyThrows;

public class Assertions {

  @SneakyThrows
  public static void assertDirectories(File dirA, File dirB) {
    File[] fileList1 = dirA.listFiles();
    File[] fileList2 = dirB.listFiles();
    Arrays.sort(fileList1);
    Arrays.sort(fileList2);
    HashMap<String, File> map1;
    if (fileList1.length < fileList2.length) {
      map1 = new HashMap<String, File>();
      for (int i = 0; i < fileList1.length; i++) {
        map1.put(fileList1[i].getName(), fileList1[i]);
      }

      compareNow(fileList2, map1);
    } else {
      map1 = new HashMap<String, File>();
      for (int i = 0; i < fileList2.length; i++) {
        map1.put(fileList2[i].getName(), fileList2[i]);
      }
      compareNow(fileList1, map1);
    }
  }

  private static void compareNow(File[] fileArr, HashMap<String, File> map) throws IOException {
    for (int i = 0; i < fileArr.length; i++) {
      String fName = fileArr[i].getName();
      File fComp = map.get(fName);
      map.remove(fName);
      if (fComp != null) {
        if (fComp.isDirectory()) {
          assertDirectories(fileArr[i], fComp);
        } else {
          String cSum1 = checksum(fileArr[i]);
          String cSum2 = checksum(fComp);
          if (!cSum1.equals(cSum2)) {
            fail(fileArr[i].getName() + "\t\t" + "different");
          } else {
            System.out.println(fileArr[i].getName() + "\t\t" + "identical");
          }
        }
      } else {
        if (fileArr[i].isDirectory()) {
          traverseDirectory(fileArr[i]);
        } else {
          fail(fileArr[i].getName() + "\t\t" + "only in " + fileArr[i].getParent());
        }
      }
    }
    Set<String> set = map.keySet();
    Iterator<String> it = set.iterator();
    while (it.hasNext()) {
      String n = it.next();
      File fileFrmMap = map.get(n);
      map.remove(n);
      if (fileFrmMap.isDirectory()) {
        traverseDirectory(fileFrmMap);
      } else {
        fail(fileFrmMap.getName() + "\t\t" + "only in " + fileFrmMap.getParent());
      }
    }
  }

  private static void traverseDirectory(File dir) {
    File[] list = dir.listFiles();
    for (int k = 0; k < list.length; k++) {
      if (list[k].isDirectory()) {
        traverseDirectory(list[k]);
      } else {
        fail(list[k].getName() + "\t\t" + "only in " + list[k].getParent());
      }
    }
  }

  private static String checksum(File file) {
    try {
      InputStream fin = new FileInputStream(file);
      java.security.MessageDigest md5er = MessageDigest.getInstance("MD5");
      byte[] buffer = new byte[1024];
      int read;
      do {
        read = fin.read(buffer);
        if (read > 0) md5er.update(buffer, 0, read);
      } while (read != -1);
      fin.close();
      byte[] digest = md5er.digest();
      if (digest == null) return null;
      String strDigest = "0x";
      for (int i = 0; i < digest.length; i++) {
        strDigest += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1).toUpperCase();
      }
      return strDigest;
    } catch (Exception e) {
      return null;
    }
  }

}