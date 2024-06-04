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

public class Splitters {
  public static final Splitter WHITESPACE = Splitter.on(" ");
  public static final Splitter TAB = Splitter.on("\t");
  public static final Splitter NEWLINE = Splitter.on("\n");
  public static final Splitter SLASH = Splitter.on("/");
  public static final Splitter DOT = Splitter.on(".");
  public static final Splitter DASH = Splitter.on("-");
  public static final Splitter UNDERSCORE = Splitter.on("_");
  public static final Splitter COLON = Splitter.on(":");
  public static final Splitter COMMA = Splitter.on(",");
  public static final Splitter SEMICOLON = Splitter.on(";");
  public static final Splitter HASHTAG = Splitter.on("#");
  public static final Splitter DOUBLE_DASH = Splitter.on("--");
  public static final Splitter PATH;
  public static final Splitter EXTENSION;
  public static final Splitter NAMESPACING;
  public static final Splitter CREDENTIALS;

  public Splitters() {}

  public static final Joiner getCorrespondingJoiner(@NonNull Splitter splitter) {
    if (splitter == null) {
      throw new NullPointerException("splitter");
    } else if (splitter.equals(WHITESPACE)) {
      return Joiners.WHITESPACE;
    } else if (splitter.equals(TAB)) {
      return Joiners.TAB;
    } else if (splitter.equals(NEWLINE)) {
      return Joiners.NEWLINE;
    } else if (splitter.equals(DASH)) {
      return Joiners.DASH;
    } else if (splitter.equals(UNDERSCORE)) {
      return Joiners.UNDERSCORE;
    } else if (splitter.equals(COMMA)) {
      return Joiners.COMMA;
    } else if (splitter.equals(SEMICOLON)) {
      return Joiners.SEMICOLON;
    } else if (splitter.equals(HASHTAG)) {
      return Joiners.HASHTAG;
    } else if (splitter.equals(SLASH)) {
      return Joiners.SLASH;
    } else if (splitter.equals(DOT)) {
      return Joiners.DOT;
    } else if (splitter.equals(COLON)) {
      return Joiners.COLON;
    } else if (splitter.equals(DOUBLE_DASH)) {
      return Joiners.DOUBLE_DASH;
    } else if (ImmutableSet.of(PATH).contains(splitter)) {
      return Joiners.SLASH;
    } else if (ImmutableSet.of(EXTENSION, NAMESPACING).contains(splitter)) {
      return Joiners.DOT;
    } else if (ImmutableSet.of(CREDENTIALS).contains(splitter)) {
      return Joiners.COLON;
    } else {
      throw new UnsupportedOperationException(String.format("Unsupported yet: '%s'", splitter));
    }
  }

  static {
    PATH = SLASH;
    EXTENSION = DOT;
    NAMESPACING = DOT;
    CREDENTIALS = COLON;
  }
}
