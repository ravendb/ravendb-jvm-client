package net.ravendb.abstractions.closure;

/**
 * Function with no arguments, which returns object of type <T>
 * @param <T>
 */
public interface Function0<T> {
  /**
   * Applies function
   * @return function result
   */
  T apply();
}
