package net.ravendb.client.documents.changes;

public interface IDatabaseChanges extends IConnectableChanges<IDatabaseChanges> {
    /**
     * Subscribe to changes for specified index only.
     * @param indexName The index name
     * @return Changes observable
     */
    IChangesObservable<IndexChange> forIndex(String indexName);

    /**
     * Subscribe to changes for specified document only.
     * @param docId Document identifier
     * @return Changes observable
     */
    IChangesObservable<DocumentChange> forDocument(String docId);

    /**
     * Subscribe to changes for all documents.
     * @return Changes observable
     */
    IChangesObservable<DocumentChange> forAllDocuments();

    /**
     * Subscribe to changes for specified operation only.
     * @param operationId Operation id
     * @return Changes observable
     */
    IChangesObservable<OperationStatusChange> forOperationId(long operationId);

    /**
     * Subscribe to change for all operation statuses.
     * @return Changes observable
     */
    IChangesObservable<OperationStatusChange> forAllOperations();


    /**
     * Subscribe to changes for all indexes.
     * @return Changes observable
     */
    IChangesObservable<IndexChange> forAllIndexes();

    /**
     * Subscribe to changes for all documents that Id starts with given prefix.
     * @param docIdPrefix The document prefix
     * @return Changes observable
     */
    IChangesObservable<DocumentChange> forDocumentsStartingWith(String docIdPrefix);

    /**
     * Subscribe to changes for all documents that belong to specified collection (Raven-Entity-Name).
     * @param collectionName The collection name.
     * @return Changes observable
     */
    IChangesObservable<DocumentChange> forDocumentsInCollection(String collectionName);

    /**
     * Subscribe to changes for all documents that belong to specified collection (Raven-Entity-Name).
     * @param clazz The document class
     * @return Changes observable
     */
    IChangesObservable<DocumentChange> forDocumentsInCollection(Class<?> clazz);

    /**
     * Subscribe for changes for all counters.
     * @return Changes observable
     */
    IChangesObservable<CounterChange> forAllCounters();

    /**
     * Subscribe to changes for all counters with a given name.
     * @param counterName Counter name
     * @return Changes observable
     */
    IChangesObservable<CounterChange> forCounter(String counterName);

    /**
     * Subscribe to changes for counter from a given document and with given name.
     * @param documentId Document identifier
     * @param counterName Counter name
     * @return Changes observable
     */
    IChangesObservable<CounterChange> forCounterOfDocument(String documentId, String counterName);

    /**
     * Subscribe to changes for all counters from a given document.
     * @param documentId Document identifier
     * @return Changes observable
     */
    IChangesObservable<CounterChange> forCountersOfDocument(String documentId);

}
