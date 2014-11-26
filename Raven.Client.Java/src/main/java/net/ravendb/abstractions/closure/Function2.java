package net.ravendb.abstractions.closure;

public interface Function2<F, G, T> {
  /**
   * Applies function
   * @param first
   * @param second
   * @return function result
   */
  T apply(F first, G second);
}
