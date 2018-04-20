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
package bio.overture.score.client.config;

import bio.overture.score.client.transport.MemoryMappedParallelPartObjectTransport;
import bio.overture.score.client.transport.ParallelPartObjectTransport;
import bio.overture.score.client.transport.PipedParallelPartObjectTransport;
import bio.overture.score.client.transport.SequentialPartObjectTransport;
import bio.overture.score.client.transport.StorageService;
import bio.overture.score.client.transport.Transport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * Configurations for data transport
 */
@Slf4j
@Configuration
public class TransportConfig {

  @Autowired
  TransportProperties properties;
  @Autowired
  StorageService proxy;

  @Bean
  public Transport.Builder builder() {
    Transport.Builder builder;
    switch (properties.getFileFrom()) {
    case "memory":
      log.debug("Transport: {}", "Memory");
      builder = MemoryMappedParallelPartObjectTransport.builder()
          .withMemory(properties.getMemory() * 1024 * 1024 * 1024)
          .withNumberOfWorkerThreads(properties.getParallel())
          .withProxy(proxy);
      break;
    case "remote":
      log.debug("Transport: {}", "Remote");
      builder =
          ParallelPartObjectTransport.builder()
              .withMemory(properties.getMemory() * 1024 * 1024 * 1024)
              .withNumberOfWorkerThreads(properties.getParallel())
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
          .withMemory(properties.getMemory() * 1024 * 1024 * 1024)
          .withNumberOfWorkerThreads(properties.getParallel())
          .withProxy(proxy);
      log.debug("Transport: {}, Builder: {}", "local", builder);

    }

    return builder;
  }

}
