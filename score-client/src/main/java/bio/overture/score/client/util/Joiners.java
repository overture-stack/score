package bio.overture.score.client.util;

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
