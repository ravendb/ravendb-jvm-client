package net.ravendb.client.documents.session;

import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;

import java.util.Map;

public class ClusterTransactionOperations extends ClusterTransactionOperationsBase implements IClusterTransactionOperations {
    public ClusterTransactionOperations(InMemoryDocumentSessionOperations session) {
        super(session);
    }

    @Override
    public <T> CompareExchangeValue<T> getCompareExchangeValue(Class<T> clazz, String key) {
        return getCompareExchangeValueInternal(clazz, key);
    }

    @Override
    public <T> Map<String, CompareExchangeValue<T>> getCompareExchangeValues(Class<T> clazz, String[] keys) {
        return getCompareExchangeValuesInternal(clazz, keys);
    }
}
