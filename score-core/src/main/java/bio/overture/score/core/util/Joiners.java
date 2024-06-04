/*
 * Copyright (c) 2024 The Ontario Institute for Cancer Research. All rights reserved.
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
package bio.overture.score.core.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;

public class Joiners {
  public static final Joiner WHITESPACE = Joiner.on(" ");
  public static final Joiner EMPTY_STRING = Joiner.on("");
  public static final Joiner SLASH = Joiner.on("/");
  public static final Joiner TAB = Joiner.on("\t");
  public static final Joiner NEWLINE = Joiner.on("\n");
  public static final Joiner DOT = Joiner.on(".");
  public static final Joiner DASH = Joiner.on("-");
  public static final Joiner UNDERSCORE = Joiner.on("_");
  public static final Joiner COMMA = Joiner.on(",");
  public static final Joiner COLON = Joiner.on(":");
  public static final Joiner SEMICOLON = Joiner.on(";");
  public static final Joiner HASHTAG = Joiner.on("#");
  public static final Joiner DOUBLE_DASH = Joiner.on("--");
  public static final Joiner PATH;
  public static final Joiner EXTENSION;
  public static final Joiner NAMESPACING;
  public static final Joiner HOST_AND_PORT;
  public static final Joiner CREDENTIALS;
  public static final Joiner INDENT;

  public static final Splitter getCorrespondingSplitter(@NonNull Joiner joiner) {
    if (joiner == null) {
      throw new NullPointerException("joiner");
    } else if (joiner.equals(WHITESPACE)) {
      return Splitters.WHITESPACE;
    } else if (joiner.equals(TAB)) {
      return Splitters.TAB;
    } else if (joiner.equals(NEWLINE)) {
      return Splitters.NEWLINE;
    } else if (joiner.equals(DASH)) {
      return Splitters.DASH;
    } else if (joiner.equals(UNDERSCORE)) {
      return Splitters.UNDERSCORE;
    } else if (joiner.equals(COMMA)) {
      return Splitters.COMMA;
    } else if (joiner.equals(SEMICOLON)) {
      return Splitters.SEMICOLON;
    } else if (joiner.equals(HASHTAG)) {
      return Splitters.HASHTAG;
    } else if (joiner.equals(DOUBLE_DASH)) {
      return Splitters.DOUBLE_DASH;
    } else if (ImmutableSet.of(DOT, EXTENSION, NAMESPACING).contains(joiner)) {
      return Splitters.DOT;
    } else if (ImmutableSet.of(SLASH, PATH).contains(joiner)) {
      return Splitters.SLASH;
    } else if (ImmutableSet.of(COLON, CREDENTIALS, HOST_AND_PORT).contains(joiner)) {
      return Splitters.COLON;
    } else if (joiner.equals(EMPTY_STRING)) {
      throw new IllegalStateException(String.format("Cannot split using '{}'", EMPTY_STRING));
    } else {
      throw new UnsupportedOperationException(String.format("Unsupported yet: '%s'", joiner));
    }
  }

  private Joiners() {}

  static {
    PATH = SLASH;
    EXTENSION = DOT;
    NAMESPACING = DOT;
    HOST_AND_PORT = COLON;
    CREDENTIALS = COLON;
    INDENT = Joiner.on("\n\t");
  }
}
