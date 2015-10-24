package org.icgc.dcc.storage.client;

import java.util.function.Consumer;

import org.junit.Rule;
import org.springframework.boot.test.OutputCapture;

import lombok.Getter;

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
