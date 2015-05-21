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

package collaboratory.storage.object.transport;

import java.io.IOException;
import java.io.InputStream;

import lombok.extern.slf4j.Slf4j;
import collaboratory.storage.object.store.core.model.DataChannel;

import com.google.common.io.ByteStreams;

/**
 * Abstract channel for data upload
 */
@Slf4j
public abstract class AbstractDataChannel implements DataChannel {

  @Override
  public boolean isValidMd5(String expectedMd5) throws IOException {
    writeTo(ByteStreams.nullOutputStream());
    if (!getMd5().equals(expectedMd5)) {
      log.warn("md5 failed. Expected: {}, Actual: {}. Resend part number = {}", expectedMd5, getMd5());
      return false;
    }
    return true;
  }

  @Override
  public void writeTo(InputStream is) throws IOException {
    throw new AssertionError("Not implemented");
  }
}
