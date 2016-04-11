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
package org.icgc.dcc.storage.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.icgc.dcc.storage.core.model.DataChannel;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ForwardingDataChannel implements DataChannel {

  private final DataChannel delegate;

  @Override
  public void writeTo(OutputStream os) throws IOException {
    delegate.writeTo(os);
  }

  @Override
  public void readFrom(InputStream is) throws IOException {
    delegate.readFrom(is);
  }

  @Override
  public void reset() throws IOException {
    delegate.reset();
  }

  @Override
  public long getLength() {
    return delegate.getLength();
  }

  @Override
  public String getMd5() {
    return delegate.getMd5();
  }

  @Override
  public boolean verifyMd5(String expectedMd5) throws IOException {
    return delegate.verifyMd5(expectedMd5);
  }

  @Override
  public void commitToDisk() {
    delegate.commitToDisk();
  }

}
