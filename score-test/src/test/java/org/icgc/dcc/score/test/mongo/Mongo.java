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
package org.icgc.dcc.score.test.mongo;

import static de.flapdoodle.embed.mongo.Command.MongoD;
import static de.flapdoodle.embed.mongo.distribution.Version.Main.V2_4;

import java.io.IOException;
import java.net.UnknownHostException;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.ArtifactStoreBuilder;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.RuntimeConfigBuilder;
import de.flapdoodle.embed.mongo.config.Timeout;
import de.flapdoodle.embed.mongo.distribution.Version.Main;
import de.flapdoodle.embed.process.config.io.ProcessOutput;
import de.flapdoodle.embed.process.io.NullProcessor;
import de.flapdoodle.embed.process.store.IArtifactStore;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Mongo {

  /**
   * Current DCC version.
   */
  private static final Main VERSION = V2_4;

  /**
   * Mongo state.
   */
  private MongodExecutable mongodExecutable;
  private MongodProcess mongodProcess;
  private MongoClient mongo;

  public int getPort() {
    return mongo.getAddress().getPort();
  }

  public MongoClient getMongo() {
    return mongo;
  }

  @SneakyThrows
  public void start() {
    mongodExecutable = createExecutable(VERSION);
    mongodProcess = createProcess(mongodExecutable);
    mongo = createClient(mongodProcess);

    log.info("Started mongo on port {}", getPort());
  }

  public void stop() {
    try {
      if (mongo != null) {
        mongo.close();
      }
    } finally {
      try {
        if (mongodProcess != null) {
          mongodProcess.stop();
        }
      } finally {
        if (mongodExecutable != null) {
          mongodExecutable.stop();
        }
      }
    }
  }

  private static MongodExecutable createExecutable(Main version) throws UnknownHostException, IOException {
    // See https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/71
    val config = new MongodConfigBuilder()
        .version(version)
        .timeout(new Timeout(60000))
        .build();

    return createStarter().prepare(config);
  }

  private static MongodProcess createProcess(MongodExecutable mongodExecutable) throws IOException {
    return mongodExecutable.start();
  }

  private static MongodStarter createStarter() {
    val config = new RuntimeConfigBuilder()
        .defaults(MongoD)
        .artifactStore(createArtifactStore())
        .processOutput(createProcessOutput())
        .build();

    return MongodStarter.getInstance(config);
  }

  private static IArtifactStore createArtifactStore() {
    // See https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo/issues/71
    val cache = false;
    return new ArtifactStoreBuilder().defaults(MongoD).useCache(cache).build();
  }

  private static ProcessOutput createProcessOutput() {
    val noop = new NullProcessor();
    return new ProcessOutput(noop, noop, noop);
  }

  private static MongoClient createClient(MongodProcess mongodProcess) throws UnknownHostException {
    val net = mongodProcess.getConfig().net();
    val address = new ServerAddress(net.getServerAddress(), net.getPort());

    return new MongoClient(address);
  }

}
