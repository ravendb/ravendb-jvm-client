package net.ravendb.client.documents.session;

import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;

import java.util.Map;

public interface IClusterTransactionOperations extends IClusterTransactionOperationsBase {
    <T> CompareExchangeValue<T> getCompareExchangeValue(Class<T> clazz, String key);
    <T> Map<String, CompareExchangeValue<T>> getCompareExchangeValues(Class<T> clazz, String[] keys);
}
