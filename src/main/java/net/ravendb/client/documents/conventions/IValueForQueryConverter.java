package net.ravendb.client.documents.conventions;

import net.ravendb.client.primitives.Reference;

@FunctionalInterface
public interface IValueForQueryConverter<T> {
    boolean tryConvertValueForQuery(String fieldName, T value, boolean forRange, Reference<Object> stringValue);
}
