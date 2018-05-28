package net.ravendb.client.documents.changes;

public interface IDatabaseChanges extends IConnectableChanges<IDatabaseChanges> {
    /**
     * Subscribe to changes for specified index only.
     * @param indexName The index name
     */
    IChangesObservable<IndexChange> forIndex(String indexName);

    /**
     * Subscribe to changes for specified document only.
     * @param docId Document identifier
     */
    IChangesObservable<DocumentChange> forDocument(String docId);

    /**
     * Subscribe to changes for all documents.
     */
    IChangesObservable<DocumentChange> forAllDocuments();

    /**
     * Subscribe to changes for specified operation only.
     * @param operationId Operation id
     */
    IChangesObservable<OperationStatusChange> forOperationId(long operationId);

    /**
     * Subscribe to change for all operation statuses.
     */
    IChangesObservable<OperationStatusChange> forAllOperations();


    /**
     * Subscribe to changes for all indexes.
     */
    IChangesObservable<IndexChange> forAllIndexes();

    /**
     * Subscribe to changes for all documents that Id starts with given prefix.
     * @param docIdPrefix The document prefix
     */
    IChangesObservable<DocumentChange> forDocumentsStartingWith(String docIdPrefix);

    /**
     * Subscribe to changes for all documents that belong to specified collection (Raven-Entity-Name).
     * @param collectionName The collection name.
     */
    IChangesObservable<DocumentChange> forDocumentsInCollection(String collectionName);

    /**
     * Subscribe to changes for all documents that belong to specified collection (Raven-Entity-Name).
     * @param clazz The document class
     */
    IChangesObservable<DocumentChange> forDocumentsInCollection(Class<?> clazz);

    /**
     * Subscribe to changes for all documents that belong to specified type (Raven-Java-Type).
     * @param typeName Java class name
     */
    IChangesObservable<DocumentChange> forDocumentsOfType(String typeName);

    /**
     * Subscribe to changes for all documents that belong to specified type (Raven-Java-Type).
     * @param clazz Java class
     */
    IChangesObservable<DocumentChange> forDocumentsOfType(Class<?> clazz);
}
