package net.ravendb.client.documents.session;

import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.operations.compareExchange.GetCompareExchangeValueOperation;
import net.ravendb.client.documents.operations.compareExchange.GetCompareExchangeValuesOperation;

import java.util.HashMap;
import java.util.Map;

public abstract class ClusterTransactionOperationsBase {
    private final InMemoryDocumentSessionOperations _session;

    public static class StoredCompareExchange {
        public final Object entity;
        public final long index;

        public StoredCompareExchange(long index, Object entity) {
            this.entity = entity;
            this.index = index;
        }
    }

    private Map<String, StoredCompareExchange> _storeCompareExchange;

    public Map<String, StoredCompareExchange> getStoreCompareExchange() {
        return _storeCompareExchange;
    }

    private Map<String, Long> _deleteCompareExchange;

    public Map<String, Long> getDeleteCompareExchange() {
        return _deleteCompareExchange;
    }

    public boolean hasCommands() {
        return _deleteCompareExchange != null || _storeCompareExchange != null;
    }

    public ClusterTransactionOperationsBase(InMemoryDocumentSessionOperations session) {
        if (session.getTransactionMode() != TransactionMode.CLUSTER_WIDE) {
            throw new IllegalStateException("This function is part of cluster transaction session, in order to use it you have to open the Session with ClusterWide option.");
        }

        _session = session;
    }

    public <T> void createCompareExchangeValue(String key, T item) {
        if (_storeCompareExchange == null) {
            _storeCompareExchange = new HashMap<>();
        }

        ensureNotDeleted(key);
        ensureNotStored(key);

        _storeCompareExchange.put(key, new StoredCompareExchange(0, item));
    }

    public <T> void updateCompareExchangeValue(CompareExchangeValue<T> item) {
        ensureNotDeleted(item.getKey());

        if (_storeCompareExchange == null) {
            _storeCompareExchange = new HashMap<>();
        }

        _storeCompareExchange.put(item.getKey(), new StoredCompareExchange(item.getIndex(), item.getValue()));
    }

    public <T> void deleteCompareExchangeValue(CompareExchangeValue<T> item) {
        ensureNotStored(item.getKey());

        if (_deleteCompareExchange == null) {
            _deleteCompareExchange = new HashMap<>();
        }

        _deleteCompareExchange.put(item.getKey(), item.getIndex());
    }

    public void deleteCompareExchangeValue(String key, long index) {
        ensureNotStored(key);

        if (_deleteCompareExchange == null) {
            _deleteCompareExchange = new HashMap<>();
        }

        _deleteCompareExchange.put(key, index);
    }

    public void clear() {
        _deleteCompareExchange = null;
        _storeCompareExchange = null;
    }

    protected <T> CompareExchangeValue<T> getCompareExchangeValueInternal(Class<T> clazz, String key) {
        return _session.getOperations().send(new GetCompareExchangeValueOperation<>(clazz, key), _session.sessionInfo);
    }

    protected <T> Map<String, CompareExchangeValue<T>> getCompareExchangeValuesInternal(Class<T> clazz, String[] keys) {
        return _session.getOperations().send(new GetCompareExchangeValuesOperation<>(clazz, keys), _session.sessionInfo);
    }

    protected void ensureNotDeleted(String key) {
        if (_deleteCompareExchange != null && _deleteCompareExchange.containsKey(key)) {
            throw new IllegalArgumentException("The key '" + key + "' already exists in the deletion requests.");
        }
    }

    protected void ensureNotStored(String key) {
        if (_storeCompareExchange != null && _storeCompareExchange.containsKey(key)) {
            throw new IllegalArgumentException("The key '" + key + "' already exists in the store requests.");
        }
    }
}
