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
package org.icgc.dcc.storage.server.repository.azure;

import lombok.Setter;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.storage.core.model.ObjectSpecification;
import org.icgc.dcc.storage.core.model.Part;
import org.icgc.dcc.storage.server.repository.UploadPartDetail;
import org.icgc.dcc.storage.server.repository.UploadStateStore;

/**
 * The Azure upload logic currently does not make use of a State Store
 * 
 */
@Setter
public class AzureUploadStateStore implements UploadStateStore {

  @Override
  public void create(ObjectSpecification spec) {
    // TODO Auto-generated method stub

  }

  @Override
  public ObjectSpecification read(String objectId, String uploadId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void delete(String objectId, String uploadId) {
    // TODO Auto-generated method stub

  }

  @Override
  public void deletePart(String objectId, String uploadId, int partNumber) {
    // TODO Auto-generated method stub

  }

  @Override
  public void markCompletedParts(String objectId, String uploadId, List<Part> parts) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean isCompleted(String objectId, String uploadId) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void finalizeUploadPart(String objectId, String uploadId, int partNumber, String md5, String eTag) {
    // TODO Auto-generated method stub

  }

  @Override
  public Map<Integer, UploadPartDetail> getUploadStatePartDetails(String objectId, String uploadId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getUploadId(String objectId) {
    // TODO Auto-generated method stub
    return null;
  }
}
