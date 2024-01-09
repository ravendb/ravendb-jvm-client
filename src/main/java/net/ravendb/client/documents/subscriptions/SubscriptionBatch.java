package net.ravendb.client.documents.subscriptions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.identity.GenerateEntityIdOnTheClient;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.http.RequestExecutor;
import org.apache.commons.logging.Log;

public class SubscriptionBatch<T> extends SubscriptionBatchBase<T> {

    private final IDocumentStore _store;
    private final GenerateEntityIdOnTheClient _generateEntityIdOnTheClient;

    private boolean _sessionOpened = false;

    public SubscriptionBatch(Class<T> clazz, boolean revisions, RequestExecutor requestExecutor, IDocumentStore store, String dbName, Log logger) {
        super(clazz, revisions, requestExecutor, dbName, logger);
        _store = store;

        _generateEntityIdOnTheClient = new GenerateEntityIdOnTheClient(requestExecutor.getConventions(), entity -> { throw new IllegalStateException("Shouldn't be generating new ids here"); });
    }


    public IDocumentSession openSession() {
        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setDatabase(_dbName);
        sessionOptions.setRequestExecutor(_requestExecutor);
        return openSessionInternal(sessionOptions);
    }

    public IDocumentSession openSession(SessionOptions options) {
        validateSessionOptions(options);

        options.setDatabase(_dbName);
        options.setRequestExecutor(_requestExecutor);

        return openSessionInternal(options);
    }

    private IDocumentSession openSessionInternal(SessionOptions options) {
        if (_sessionOpened) {
            throwSessionCanBeOpenedOnlyOnce();
        }
        _sessionOpened = true;
        IDocumentSession s = _store.openSession(options);

        loadDataToSession((InMemoryDocumentSessionOperations) s);
        return s;
    }

    private void throwSessionCanBeOpenedOnlyOnce() {
        throw new IllegalStateException("Session can only be opened once per each Subscription batch");
    }

    private static void validateSessionOptions(SessionOptions options) {
        if (options.getDatabase() != null) {
            throw new IllegalStateException("Cannot set Database when session is opened in subscription.");
        }

        if (options.getRequestExecutor() != null) {
            throw new IllegalStateException("Cannot set RequestExecutor when session is opened in subscription.");
        }

        if (options.getTransactionMode() != TransactionMode.SINGLE_NODE) {
            throw new IllegalStateException("Cannot set TransactionMode when session is opened in subscription. Only SINGLE_NODE is supported.");
        }
    }

    private void loadDataToSession(InMemoryDocumentSessionOperations s) {
        if (s.noTracking) {
            return;
        }

        if (_includes != null && !_includes.isEmpty()) {
            for (ObjectNode item : _includes) {
                s.registerIncludes(item);
            }
        }

        if (_counterIncludes != null && !_counterIncludes.isEmpty()) {
            for (BatchFromServer.CounterIncludeItem item : _counterIncludes) {
                s.registerCounters(item.getIncludes(), item.getCounterIncludes());
            }
        }

        if (_timeSeriesIncludes != null && !_timeSeriesIncludes.isEmpty()) {
            for (ObjectNode item : _timeSeriesIncludes) {
                s.registerTimeSeries(item);
            }
        }

        for (Item<T> item : getItems()) {
            if (item.isProjection() || item.isRevision()) {
                continue;
            }

            DocumentInfo documentInfo = new DocumentInfo();
            documentInfo.setId(item.getId());
            documentInfo.setDocument(item.getRawResult());
            documentInfo.setMetadata(item.getRawMetadata());
            documentInfo.setMetadataInstance(item.getMetadata());
            documentInfo.setChangeVector(item.getChangeVector());
            documentInfo.setEntity(item.getResult());
            documentInfo.setNewDocument(false);
            s.registerExternalLoadedIntoTheSession(documentInfo);
        }

    }

    @Override
    protected void ensureDocumentId(T item, String id) {
        _generateEntityIdOnTheClient.trySetIdentity(item, id);
    }
}
