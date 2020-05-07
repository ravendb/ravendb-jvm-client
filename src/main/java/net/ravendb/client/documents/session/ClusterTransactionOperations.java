package net.ravendb.client.documents.session;

import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.session.operations.lazy.LazyClusterTransactionOperations;

import java.util.Map;

public class ClusterTransactionOperations extends ClusterTransactionOperationsBase implements IClusterTransactionOperations {
    public ClusterTransactionOperations(DocumentSession session) {
        super(session);
    }

    @Override
    public ILazyClusterTransactionOperations lazily() {
        return new LazyClusterTransactionOperations(session);
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
