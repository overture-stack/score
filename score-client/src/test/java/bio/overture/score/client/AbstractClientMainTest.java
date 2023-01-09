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
package bio.overture.score.client;

import lombok.Getter;
import org.junit.Rule;
import org.springframework.boot.test.rule.OutputCapture;

import java.util.function.Consumer;

public abstract class AbstractClientMainTest {

  @Rule
  public OutputCapture capture = new OutputCapture();
  public ExitCodeCapture exitCodeCapture = new ExitCodeCapture();

  protected void executeMain(String... args) {
    ClientMain.exit = exitCodeCapture;
    ClientMain.main(args);
  }

  protected String getOutput() {
    return capture.toString();
  }

  protected Integer getExitCode() {
    return exitCodeCapture.getExitCode();
  }

  private static class ExitCodeCapture implements Consumer<Integer> {

    @Getter
    private Integer exitCode;

    @Override
    public void accept(Integer exitCode) {
      this.exitCode = exitCode;
    }

  }

}
