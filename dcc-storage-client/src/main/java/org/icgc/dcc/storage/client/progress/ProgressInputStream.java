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
package org.icgc.dcc.storage.client.progress;

import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.io.InputStream;

import org.icgc.dcc.storage.core.util.ForwardingInputStream;

public class ProgressInputStream extends ForwardingInputStream {

  private final Progress progress;

  public ProgressInputStream(@NonNull InputStream inputStream, @NonNull Progress progress) {
    super(inputStream);
    this.progress = progress;
  }

  @Override
  public int read() throws IOException {
    val value = super.read();
    if (value > 0) {
      progress.incrementBytesRead(1);
    }

    return value;
  }

  @Override
  public int read(byte[] b) throws IOException {
    val value = super.read(b);
    if (value > 0) {
      progress.incrementBytesRead(value);
    }

    return value;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    val value = super.read(b, off, len);
    if (value > 0) {
      progress.incrementBytesRead(value);
    }

    return value;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

}