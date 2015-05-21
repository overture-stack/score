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
package collaboratory.storage.object.store.client.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import collaboratory.storage.object.store.client.upload.ObjectStoreServiceProxy;
import collaboratory.storage.object.transport.MemoryMappedParallelPartObjectTransport;
import collaboratory.storage.object.transport.ObjectTransport;
import collaboratory.storage.object.transport.ParallelPartObjectTransport;
import collaboratory.storage.object.transport.PipedParallelPartObjectTransport;
import collaboratory.storage.object.transport.SequentialPartObjectTransport;

/**
 * Configurations for data transport
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "transport")
@Slf4j
public class TransportConfig {

  private String fileFrom;
  private long memory;
  private int parallel;

  @Autowired
  ObjectStoreServiceProxy proxy;

  @Bean
  public ObjectTransport.Builder TransportBuilder() {
    ObjectTransport.Builder builder;
    switch (fileFrom) {
    case "memory":
      log.debug("Transport: {}", "Memory");
      builder = MemoryMappedParallelPartObjectTransport.builder()
          .withMemory(memory * 1024 * 1024 * 1024)
          .withNumberOfWorkerThreads(parallel)
          .withProxy(proxy);
      break;
    case "remote":
      log.debug("Transport: {}", "Remote");
      builder =
          ParallelPartObjectTransport.builder()
              .withMemory(memory * 1024 * 1024 * 1024)
              .withNumberOfWorkerThreads(parallel)
              .withProxy(proxy);
      break;
    case "sequential":
      log.debug("Transport: {}", "Sequential");
      builder =
          SequentialPartObjectTransport.builder()
              .withProxy(proxy);
      break;
    default:
      builder = PipedParallelPartObjectTransport.builder()
          .withMemory(memory * 1024 * 1024 * 1024)
          .withNumberOfWorkerThreads(parallel)
          .withProxy(proxy);
      log.debug("Transport: {}, Builder: {}", "local", builder);

    }
    return builder;

  }
}
