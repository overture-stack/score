package bio.overture.score.client.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.stream.Collector;

public class Collectors {
  public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toImmutableList() {
    return Collector.of(
        ImmutableList.Builder::new,
        (builder, e) -> {
          builder.add(e);
        },
        (b1, b2) -> {
          return b1.addAll(b2.build());
        },
        (builder) -> {
          return builder.build();
        });
  }

  public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> toImmutableSet() {
    return Collector.of(
        ImmutableSet.Builder::new,
        (builder, e) -> {
          builder.add(e);
        },
        (b1, b2) -> {
          return b1.addAll(b2.build());
        },
        (builder) -> {
          return builder.build();
        });
  }

  private Collectors() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
