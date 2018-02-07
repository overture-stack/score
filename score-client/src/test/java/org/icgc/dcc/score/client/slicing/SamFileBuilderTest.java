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
package org.icgc.dcc.score.client.slicing;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.score.client.command.ViewCommand.OutputFormat;
import org.icgc.dcc.score.client.metadata.Entity;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class SamFileBuilderTest {

  SamFileBuilder sut;

  @Before
  public void setUp() {
    sut = new SamFileBuilder();
  }

  @Test
  public void test_sane_bam_filename() {
    String longName = "TEST-HCC1954.NORMAL.7x.compare.1_9000000-12500000.bam";
    String result = sut.handleMaxFilenameLength(longName, ".bam");
    assertThat(result, equalTo(longName));
    assertThat(result.length(), equalTo(longName.length()));
  }

  @Test
  public void test_long_bam_filename() {
    String longName =
        "TEST-HCC1954.NORMAL.7x.compare.1_9000000-12500000__2_180000000-205000000__4_50000-750000__5_1291650-1296560__16_25054900-25099000__21_18279635-18282565.bam";
    String fname =
        "TEST-HCC1954.NORMAL.7x.compare.1_9000000-12500000__2_180000000-205000000__4_50000-750000__5_1291650-1296560__16_25054900-250990~.bam";
    String result = sut.handleMaxFilenameLength(longName, ".bam");
    assertThat(result.length(), lessThan(longName.length()));
    assertThat(result, equalTo(fname));
  }

  @Test
  public void test_sane_bai_filename() {
    String longName = "TEST-HCC1954.NORMAL.7x.compare.1_9000000-12500000.bam";
    String result = sut.handleMaxFilenameLength(longName, ".bam");
    assertThat(result, equalTo(longName));
    assertThat(result.length(), equalTo(longName.length()));
  }

  @Test
  public void test_long_bai_filename() {
    String longName =
        "TEST-HCC1954.NORMAL.7x.compare.1_9000000-12500000__2_180000000-205000000__4_50000-750000__5_1291650-1296560__16_25054900-25099000__21_18279635-18282565.bam.bai";
    String fname =
        "TEST-HCC1954.NORMAL.7x.compare.1_9000000-12500000__2_180000000-205000000__4_50000-750000__5_1291650-1296560__16_25054900-250990~.bam.bai";

    String result = sut.handleMaxFilenameLength(longName, ".bam.bai");
    assertThat(result.length(), lessThan(longName.length()));
    assertThat(result, equalTo(fname));
  }

  @Test
  public void test_output_file_extension_specified_by_output_format() {
    sut.outputFormat(OutputFormat.SAM);
    String fname = "TEST-HCC1954.NORMAL.7x.compare.10_1900000-2000000.sam"; // expected output

    Entity stub = new Entity();
    stub.setFileName("TEST-HCC1954.NORMAL.7x.compare.bam");

    String result = sut.generateOutputFileName(stub, "10:1900000-2000000");
    assertThat(result, equalTo(fname));
  }

  @Test
  public void test_single_query_filename_construction() {
    sut.outputFormat(OutputFormat.BAM);
    String fname = "TEST-HCC1954.NORMAL.7x.compare.10_1900000-2000000.bam";

    Entity stub = new Entity();
    stub.setFileName("TEST-HCC1954.NORMAL.7x.compare.bam");

    String result = sut.generateOutputFileName(stub, "10:1900000-2000000");
    assertThat(result, equalTo(fname));
  }

  @Test
  public void test_query_list_filename_construction() {
    sut.outputFormat(OutputFormat.BAM);
    String fname =
        "TEST-HCC1954.NORMAL.7x.compare.chr1_1000-2000__chr5_150000-250000__chr16_100250000-100500000.bam";

    List<String> queries = new ArrayList<String>();
    queries.add("chr1:1000-2000");
    queries.add("chr5:150000-250000");
    queries.add("chr16:100250000-100500000");

    Entity stub = new Entity();
    stub.setFileName("TEST-HCC1954.NORMAL.7x.compare.bam");

    String result = sut.generateOutputFileName(stub, queries);
    assertThat(result, equalTo(fname));
  }
}
