package net.ravendb.client.documents.changes;

public interface IIndexChanges<TChange> {

    /**
     * Subscribe to changes for specified index only.
     * @param indexName The index name
     * @return Changes observable
     */
    IChangesObservable<TChange> forIndex(String indexName);

    /**
     * Subscribe to changes for all indexes.
     * @return Changes observable
     */
    IChangesObservable<TChange> forAllIndexes();
}
