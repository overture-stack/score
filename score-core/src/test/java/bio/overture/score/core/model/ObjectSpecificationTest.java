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
package bio.overture.score.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.net.URL;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

public class ObjectSpecificationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  public void test() {
    URL url = this.getClass().getResource("/test.meta");
    File testfile = new File(url.getFile());

    try {
      val sut = MAPPER.readValue(testfile, ObjectSpecification.class);
      sut.hasPartChecksums();
      System.out.println("test");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void test_no_object_md5() {
    URL url = this.getClass().getResource("/test.meta");
    File testfile = new File(url.getFile());

    try {
      val sut = MAPPER.readValue(testfile, ObjectSpecification.class);
      Assert.assertNull(sut.getObjectMd5());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void test_add_object_md5() {
    URL url = this.getClass().getResource("/legacy.meta");
    File legacyfile = new File(url.getFile());

    try {
      val sut = MAPPER.readValue(legacyfile, ObjectSpecification.class);
      Assert.assertNull(sut.getObjectMd5());

      sut.setObjectMd5("aiwuehcfnoa9w8yrbqv90238y4cnoaq89yrnawe");

      File output = new File("/tmp/test.meta");
      MAPPER.writeValue(output, sut);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
