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
package org.icgc.dcc.score.client.manifest;

import static bio.overture.score.core.util.UUIDs.isUUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

@Getter
public class ManifestResource {

  public enum Type {

    ID, URL, FILE

  }

  private final ManifestResource.Type type;
  private final String value;

  public ManifestResource(@NonNull String value) {
    super();
    this.value = value.trim();
    this.type = resolveType(value);
  }

  private static Type resolveType(String value) {
    if (isURL(value)) {
      return Type.URL;
    } else if (isUUID(value)) {
      return Type.ID;
    } else {
      return Type.FILE;
    }
  }

  private static boolean isURL(String manifestSpec) {
    return manifestSpec.startsWith("http:/") || manifestSpec.startsWith("https:/");
  }

  public boolean isGnosManifest() {
    val isFile = resolveType(value).equals(Type.FILE);
    val isXml = value.toLowerCase().contains(".xml");
    return isFile && isXml;
  }

  @Override
  public String toString() {
    return value;
  }

}
