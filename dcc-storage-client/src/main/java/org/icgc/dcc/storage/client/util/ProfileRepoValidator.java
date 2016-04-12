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
package org.icgc.dcc.storage.client.util;

public class ProfileRepoValidator {

  /*
   * values found in DCC Portal manifest downloads
   */
  public final static String COLLABORATORY_REPO = "collaboratory";
  public final static String AWS_VIRGINIA_REPO = "aws-virginia";

  public final static String COLLABORATORY_PROFILE = "collab";
  public final static String AWS_PROFILE = "aws";

  public static boolean validateRepoAgainstProfile(String profile, String repoCode) {
    if (profile.equalsIgnoreCase(COLLABORATORY_PROFILE)) {
      return repoCode.equalsIgnoreCase(COLLABORATORY_REPO);
    }

    if (profile.equalsIgnoreCase(AWS_PROFILE)) {
      return repoCode.equalsIgnoreCase(AWS_VIRGINIA_REPO);
    }

    throw new IllegalArgumentException(String.format(
        "Didn't recognize Client Profile/Repository combination '%s'/'%s'", profile, repoCode));
  }
}
