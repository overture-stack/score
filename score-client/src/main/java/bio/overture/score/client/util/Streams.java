package bio.overture.score.client.util;

import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NonNull;

public class Streams {
  public static <T> Stream<T> stream(@NonNull Iterable<T> iterable) {
    if (iterable == null) {
      throw new NullPointerException("iterable");
    } else {
      return StreamSupport.stream(iterable.spliterator(), false);
    }
  }

  @SafeVarargs
  public static <T> Stream<T> stream(@NonNull T... values) {
    if (values == null) {
      throw new NullPointerException("values");
    } else {
      return ImmutableList.copyOf(values).stream();
    }
  }

  private Streams() {}
}
