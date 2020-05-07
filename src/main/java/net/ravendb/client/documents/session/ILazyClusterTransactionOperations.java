package net.ravendb.client.documents.session;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;

import java.util.Map;
import java.util.function.Consumer;

public interface ILazyClusterTransactionOperations {
    <T> Lazy<CompareExchangeValue<T>> getCompareExchangeValue(Class<T> clazz, String key);

    <T> Lazy<CompareExchangeValue<T>> getCompareExchangeValue(Class<T> clazz, String key, Consumer<CompareExchangeValue<T>> onEval);

    <T> Lazy<Map<String, CompareExchangeValue<T>>> getCompareExchangeValues(Class<T> clazz, String[] keys);

    <T> Lazy<Map<String, CompareExchangeValue<T>>> getCompareExchangeValues(Class<T> clazz, String[] keys, Consumer<Map<String, CompareExchangeValue<T>>> onEval);
}
