package net.ravendb.abstractions.closure;

public interface Function3<F, G, H, T> {
  /**
   * Applies function
   * @param first
   * @param second
   * @param third
   * @return function result
   */
  T apply(F first, G second, H third);
}
