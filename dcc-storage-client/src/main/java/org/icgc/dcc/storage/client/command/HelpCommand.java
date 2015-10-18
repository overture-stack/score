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
package org.icgc.dcc.storage.client.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

import lombok.val;

/**
 * Resolves URL for a supplied object id.
 */
@Component
@Parameters(separators = "=", commandDescription = "Displays help information")
public class HelpCommand extends AbstractClientCommand {

  /**
   * Dependencies.
   */
  @Autowired
  private JCommander cli;

  @Override
  public int execute() throws Exception {
    title();
    usage();
    return SUCCESS_STATUS;
  }

  private void usage() {
    val builder = new StringBuilder();
    cli.usage(builder);
    String text = builder.toString();

    // Options
    text = text.replaceAll("(--\\S+)", "@|bold $1|@");

    // Sections
    text = text.replaceAll("(Options:)", "@|green $1|@");
    text = text.replaceAll("(Commands:)", "@|green $1|@");

    // Commands
    text = text.replaceAll("(download      )", "@|blue $1|@");
    text = text.replaceAll("(help      )", "@|blue $1|@");
    text = text.replaceAll("(manifest      )", "@|blue $1|@");
    text = text.replaceAll("(mount      )", "@|blue $1|@");
    text = text.replaceAll("(upload      )", "@|blue $1|@");
    text = text.replaceAll("(url      )", "@|blue $1|@");
    text = text.replaceAll("(version      )", "@|blue $1|@");
    text = text.replaceAll("(view      )", "@|blue $1|@");

    terminal.println(terminal.ansi(text));
  }

}
