package net.ravendb.client.documents.subscriptions;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.*;

import java.security.KeyStore;

public class SubscriptionWorker<T> extends AbstractSubscriptionWorker<SubscriptionBatch<T>, T> implements CleanCloseable {
    private final DocumentStore _store;

    SubscriptionWorker(Class<?> clazz, SubscriptionWorkerOptions options, boolean withRevisions, DocumentStore documentStore, String dbName) {
        super(clazz, options, withRevisions, documentStore.getEffectiveDatabase(dbName), documentStore.getExecutorService());

        _store = documentStore;
    }

    @Override
    protected RequestExecutor getRequestExecutor() {
        return _store.getRequestExecutor(_dbName);
    }

    @Override
    protected void setLocalRequestExecutor(String url, KeyStore cert, char[] password, KeyStore trustStore) {
        if (_subscriptionLocalRequestExecutor != null) {
            _subscriptionLocalRequestExecutor.close();
        }
        _subscriptionLocalRequestExecutor = RequestExecutor.createForSingleNodeWithoutConfigurationUpdates(
                url,
                _dbName, cert, password, trustStore,
                _store.getExecutorService(),
                _store.getConventions());

        _store.registerEvents(_subscriptionLocalRequestExecutor);
    }

    @Override
    protected SubscriptionBatch<T> createEmptyBatch() {
        return new SubscriptionBatch<>(_clazz, _revisions, _subscriptionLocalRequestExecutor, _store, _dbName, _logger);
    }

    @Override
    protected void trySetRedirectNodeOnConnectToServer() {
        // no-op
    }
}
