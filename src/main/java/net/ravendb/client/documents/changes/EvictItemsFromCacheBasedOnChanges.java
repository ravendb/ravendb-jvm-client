package net.ravendb.client.documents.changes;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.ExceptionsUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class EvictItemsFromCacheBasedOnChanges implements CleanCloseable, IObserver<DatabaseChange> {

    @SuppressWarnings("FieldCanBeLocal")
    private final String _databaseName;
    private final DatabaseChanges _changes;
    private CleanCloseable _documentsSubscription;
    private CleanCloseable _indexesSubscription;
    private final RequestExecutor _requestExecutor;
    private CompletableFuture _taskConnected;
    private CleanCloseable _aggressiveCachingSubscription;

    @SuppressWarnings("unchecked")
    public EvictItemsFromCacheBasedOnChanges(DocumentStore store, String databaseName) {
        _databaseName = databaseName;
        _requestExecutor = store.getRequestExecutor(databaseName);
        _changes = new DatabaseChanges(_requestExecutor, databaseName, store.getExecutorService(), null, null);

        _taskConnected = CompletableFuture.runAsync(this::ensureConnectedInternal, store.getExecutorService());
    }

    @Override
    public void onNext(DatabaseChange value) {
        if (value instanceof DocumentChange) {
            DocumentChange documentChange = (DocumentChange) value;
            if (AggressiveCacheChange.shouldUpdateAggressiveCache(documentChange)) {
                _requestExecutor.getCache().generation.incrementAndGet();
            }
        } else if (value instanceof IndexChange) {
            IndexChange indexChange = (IndexChange) value;
            if (AggressiveCacheChange.shouldUpdateAggressiveCache(indexChange)) {
                _requestExecutor.getCache().generation.incrementAndGet();
            }
        } else if (value instanceof AggressiveCacheChange) {
            AggressiveCacheChange aggressiveCacheChange = (AggressiveCacheChange) value;
            _requestExecutor.getCache().generation.incrementAndGet();

        }
    }

    @Override
    public void onError(Exception error) {
    }

    @Override
    public void onCompleted() {
    }

    public void close() {
        try (CleanCloseable changesScope = _changes) {
            if (_documentsSubscription != null) {
                _documentsSubscription.close();
            }
            if (_indexesSubscription != null) {
                _indexesSubscription.close();
            }
            if (_aggressiveCachingSubscription != null) {
                _aggressiveCachingSubscription.close();
            }
        }
    }

    private void ensureConnectedInternal() {
        _changes.ensureConnectedNow();

        try {
            ChangesSupportedFeatures changesSupportedFeatures = _changes.getSupportedFeatures().get();
            if (changesSupportedFeatures.isAggressiveCachingChange()) {
                IChangesObservable<AggressiveCacheChange> forAggressiveCachingChanges = _changes.forAggressiveCaching();
                _aggressiveCachingSubscription = forAggressiveCachingChanges.subscribe((IObserver<AggressiveCacheChange>) (IObserver<?>) this);
            } else {
                IChangesObservable<DocumentChange> docSub = _changes.forAllDocuments();
                _documentsSubscription = docSub.subscribe((IObserver<DocumentChange>) (IObserver<?>) this);
                IChangesObservable<IndexChange> indexSub = _changes.forAllIndexes();
                _indexesSubscription = indexSub.subscribe((IObserver<IndexChange>) (IObserver<?>) this);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw ExceptionsUtils.unwrapException(e);
        }
    }

    public void ensureConnected() {
        try {
            _taskConnected.get();
        } catch (Exception e) {
            throw ExceptionsUtils.unwrapException(e);
        }
    }


}
