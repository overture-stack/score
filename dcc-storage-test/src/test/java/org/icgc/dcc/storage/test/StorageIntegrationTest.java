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
package org.icgc.dcc.storage.test;

import static com.google.common.base.Objects.firstNonNull;
import static org.icgc.dcc.storage.test.util.SpringBootProcess.bootRun;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StorageIntegrationTest extends AbstractStorageIntegrationTest {

  @Before
  @Override
  public void setUp() throws Exception {
    banner("STORAGE INTEGRATION TEST");
    super.setUp();
  }

  @After
  @Override
  public void tearDown() {
    super.tearDown();
  }

  @Test
  public void test_end_to_end() throws InterruptedException {
    execute();
  }

  @Override
  Process storageServer() {
    int debugPort = Integer.parseInt(System.getProperty("storage.server.debugPort", "-1"));

    return bootRun(
        "dcc-storage-server",
        debugPort,
        "-Dspring.profiles.active=dev,secure,default", // Secure
        "-Dlogging.file=" + fs.getLogsDir() + "/dcc-storage-server.log",
        "-Dserver.port=" + storagePort,
        "-Dbucket.name.object=oicr.icgc.dev",
        "-Dbucket.name.state=oicr.icgc.dev.state",
        "-Dauth.server.url=https://localhost:" + authPort + "/oauth/check_token",
        "-Dauth.server.clientId=storage",
        "-Dauth.server.clientsecret=pass",
        "-Dmetadata.url=https://localhost:" + metadataPort,
        "-Dendpoints.jmx.domain=storage");
  }

  @Override
  Process storageClient(String accessToken, String... args) {
    int debugPort = Integer.parseInt(firstNonNull(System.getProperty("storage.client.debugPort"), "-1"));

    return bootRun(
        "dcc-storage-client",
        debugPort,
        args,
        "-Dlogging.file=" + fs.getLogsDir() + "/dcc-storage-client.log",
        "-Dmetadata.url=https://localhost:" + metadataPort,
        "-Dmetadata.ssl.enabled=false",
        "-Dstorage.url=http://localhost:" + storagePort,
        "-DaccessToken=" + accessToken);
  }

}
