package bio.overture.score.server.util;

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
