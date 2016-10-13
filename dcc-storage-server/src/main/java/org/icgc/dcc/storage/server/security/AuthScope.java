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
package org.icgc.dcc.storage.server.security;

import lombok.NonNull;

/**
 * Utility class modeling a Scope retrieved from dcc-Auth. Traditionally, scopes were two parts joined by a period:
 * prefix.suffix where the prefix was usually a system identifier and the suffix was the permitted operation.
 * 
 * With the Storage system, an additional level became necessary to control access to a specific project, for a given
 * operation. These scopes were of the format system.project-code.action, i.e., collab.ABC.upload
 * 
 * For backwards compatibility, we have adopted the additional convention of non-project-specific scopes covering all
 * projects. i.e., collab.upload means the scope allows write access to any file in Collaboratory regardless of project
 */
public class AuthScope {

  public static final String ALL_PROJECTS = "*";
  private String system = "";
  private String project = "";
  private String operation = "";

  /**
   * 
   * @param scopeParts
   */
  public AuthScope(String[] scopeParts) {
    system = scopeParts[0].toLowerCase();

    if (scopeParts.length == 3) {
      project = scopeParts[1].toUpperCase();
      operation = scopeParts[2].toLowerCase();
    } else {
      operation = scopeParts[1].toLowerCase();
      project = ALL_PROJECTS; // internal representation for "all projects"
    }
  }

  public static AuthScope from(@NonNull String scopeStr) {
    String[] parts = scopeStr.split("\\.");

    if ((parts.length >= 2) && (parts.length <= 3)) {
      return new AuthScope(parts);
    } else {
      throw new IllegalArgumentException(String.format("Invalid scope value received: '%s'", scopeStr));
    }
  }

  public boolean matches(AuthScope rule) {
    return (getSystem().equals(rule.getSystem()) && getOperation().equals(rule.getOperation()));
  }

  public boolean allowAllProjects() {
    return ALL_PROJECTS.equals(project);
  }

  /**
   * @return the system
   */
  public String getSystem() {
    return system;
  }

  /**
   * @return the project
   */
  public String getProject() {
    return project;
  }

  /**
   * @return the operation
   */
  public String getOperation() {
    return operation;
  }

}
