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
package bio.overture.score.client.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Lists;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static bio.overture.score.client.cli.Parameters.checkParameter;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.padEnd;
import static com.google.common.base.Strings.repeat;

@Component
@Parameters(separators = "=", commandDescription = "Display help information for a specified command name")
public class HelpCommand extends AbstractClientCommand {

  /**
   * Options.
   */
  @Parameter(description = "[command]")
  private List<String> commandNames = Lists.newArrayList();

  /**
   * Dependencies.
   */
  @Autowired
  private JCommander cli;

  @Override
  public int execute() throws Exception {
    checkParameter(commandNames.size() <= 1, "At most one command name is expected, got %s", commandNames.size());

    if (commandNames.isEmpty()) {
      printAppUsage();
    } else {
      printCommandUsage();
    }

    return SUCCESS_STATUS;
  }

  private void printAppUsage() {
    printTitle();

    printUsage("[options] [command] [command options]");

    printParams(cli);

    printHeading("Commands");
    for (val commandName : cli.getCommands().keySet()) {
      val description = cli.getCommandDescription(commandName);
      terminal.println("    " + terminal.ansi("@|blue " + padEnd(commandName, 10, ' ') + "|@") + description);
    }
  }

  private void printCommandUsage() {
    val commandName = commandNames.get(0);
    val command = cli.getCommands().get(commandName);

    val mainParam = firstNonNull(command.getMainParameterDescription(), "");
    val description = cli.getCommandDescription(commandName);
    terminal.clearLine();

    val hasOptions = !command.getParameters().isEmpty();

    printUsage(commandName + (hasOptions ? " [options] " : " ") + mainParam);
    printHeading("Command");
    terminal.println("    " + terminal.ansi("@|blue " + commandName + "|@") + "   " + description);
    if (hasOptions) {
      printParams(command);
    }
  }

  private void printParams(JCommander command) {
    printHeading("Options");
    for (val param : command.getParameters()) {
      printParam(param);
    }
  }

  private void printParam(ParameterDescription param) {
    val name = terminal.ansi("@|bold " + param.getNames() + "|@");
    val required = param.getParameter().required() ? terminal.ansi(" @|yellow *|@") : "  ";
    terminal.println(" " + required + " " + name);
    terminal.println("      " + wrap(param.getDescription(), 6));
    if (param.getDefault() != null) {
      val defaultValue = formatDefault(param.getDefault());
      terminal.println("       Default: " + defaultValue);
    }
  }

  private void printUsage(String usage) {
    terminal.println("Usage: " + APPLICATION_NAME + " " + usage);
  }

  private void printHeading(String heading) {
    terminal.println("  " + terminal.ansi("@|green " + heading + ":|@"));
  }

  private String wrap(String text, int indent) {
    val rightPadding = 10;
    val max = terminal.getWidth() - indent - rightPadding;
    String[] words = text.split(" ");
    int current = indent;
    int i = 0;

    val wrapped = new StringBuilder();
    while (i < words.length) {
      val word = words[i];
      if (word.length() > max || current + word.length() <= max) {
        wrapped.append(" ").append(word);
        current += word.length() + 1;
      } else {
        wrapped.append("\n").append(repeat(" ", indent + 1)).append(word);
        current = indent;
      }
      i++;
    }

    return wrapped.toString();
  }

  private static String formatDefault(Object defaultValue) {
    if (defaultValue == null) {
      return null;
    }
    if (Enum.class.isAssignableFrom(defaultValue.getClass())) {
      defaultValue = defaultValue.toString().toLowerCase().replaceAll("_", "-");
    }

    return defaultValue.toString();
  }

}
