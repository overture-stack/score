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

package org.icgc.dcc.score.client.transport;

import java.io.IOException;
import java.io.InputStream;

import org.icgc.dcc.score.core.model.DataChannel;

import com.google.common.io.ByteStreams;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A representation of a channel for data tranfser.
 */
@Slf4j
public abstract class AbstractDataChannel implements DataChannel {

  @Override
  public boolean verifyMd5(String expectedMd5) throws IOException {
    // Need to read through the whole stream in order to calculate the md5
    writeTo(ByteStreams.nullOutputStream());

    // Now it's available
    val actualMd5 = getMd5();
    if (!actualMd5.equals(expectedMd5)) {
      log.warn("md5 failed. Expected: {}, Actual: {}.", expectedMd5, actualMd5);
      return false;
    }
    return true;
  }

  @Override
  public void readFrom(InputStream is) throws IOException {
    throw new AssertionError("Not implemented");
  }
}
