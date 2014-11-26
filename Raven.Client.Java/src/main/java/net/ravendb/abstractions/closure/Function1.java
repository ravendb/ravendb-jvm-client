package net.ravendb.abstractions.closure;

public interface Function1<F, T> {
  /**
   * Applies function
   * @param input
   * @return function result
   */
  T apply(F input);
}
