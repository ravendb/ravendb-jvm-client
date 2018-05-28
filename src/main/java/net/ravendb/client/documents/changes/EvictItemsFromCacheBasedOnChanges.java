package net.ravendb.client.documents.changes;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;

public class EvictItemsFromCacheBasedOnChanges implements CleanCloseable, IObserver<DatabaseChange> {

    @SuppressWarnings("FieldCanBeLocal")
    private final String _databaseName;
    private final IDatabaseChanges _changes;
    private final CleanCloseable _documentsSubscription;
    private final CleanCloseable _indexesSubscription;
    private final RequestExecutor _requestExecutor;

    @SuppressWarnings("unchecked")
    public EvictItemsFromCacheBasedOnChanges(DocumentStore store, String databaseName) {
        _databaseName = databaseName;
        _changes = store.changes(databaseName);
        _requestExecutor = store.getRequestExecutor(databaseName);
        IChangesObservable<DocumentChange> docSub = _changes.forAllDocuments();
        _documentsSubscription = docSub.subscribe((IObserver<DocumentChange>)(IObserver<?>)this);
        IChangesObservable<IndexChange> indexSub = _changes.forAllIndexes();
        _indexesSubscription = indexSub.subscribe( (IObserver<IndexChange>)(IObserver<?>)this);
    }

    @Override
    public void onNext(DatabaseChange value) {
        if (value instanceof DocumentChange) {
            DocumentChange documentChange = (DocumentChange) value;
            if (documentChange.getType() == DocumentChangeTypes.PUT || documentChange.getType() == DocumentChangeTypes.DELETE) {
                _requestExecutor.getCache().generation.incrementAndGet();
            }
        } else if (value instanceof IndexChange) {
            IndexChange indexChange = (IndexChange) value;
            if (indexChange.getType() == IndexChangeTypes.BATCH_COMPLETED || indexChange.getType() == IndexChangeTypes.INDEX_REMOVED) {
                _requestExecutor.getCache().generation.incrementAndGet();
            }
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
            _documentsSubscription.close();
            _indexesSubscription.close();
        }
    }
}
