package net.ravendb.client.primitives;

@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
