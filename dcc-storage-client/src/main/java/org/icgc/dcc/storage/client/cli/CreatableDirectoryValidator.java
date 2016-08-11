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
package org.icgc.dcc.storage.client.cli;

import static org.icgc.dcc.storage.client.cli.Parameters.checkParameter;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

public class CreatableDirectoryValidator implements IValueValidator<File> {

  @Override
  public void validate(String name, File dir) throws ParameterException {
    String fullPathStr = "";

    try {
      fullPathStr = dir.getCanonicalPath();
    } catch (IOException ioe) {
      checkParameter(ioe != null, "Unable to evaluate path: %s", ioe.getMessage());
    }

    if (dir.exists() == false) {
      checkParameter(dir.mkdir(), "Invalid option: %s: %s could not be created", name, fullPathStr);
    }

    checkParameter(dir.isDirectory(), "Invalid option: %s: %s is not a directory", name, fullPathStr);
    checkParameter(dir.canWrite(), "Invalid option: %s: %s could not be written to. Please check permissions.", name,
        fullPathStr);
  }
}
