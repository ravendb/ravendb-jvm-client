package net.ravendb.abstractions.closure;

/**
 * Represents typed action with 1 argument
 * @param <X>
 */
public interface Action1<X> {
  public void apply(X first);
}
