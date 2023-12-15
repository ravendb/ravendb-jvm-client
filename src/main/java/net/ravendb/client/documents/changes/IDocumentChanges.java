package net.ravendb.client.documents.changes;

public interface IDocumentChanges<TChange> {
    /**
     * Subscribe to changes for specified document only.
     * @param docId Document identifier
     * @return Changes observable
     */
    IChangesObservable<TChange> forDocument(String docId);

    /**
     * Subscribe to changes for all documents.
     * @return Changes observable
     */
    IChangesObservable<TChange> forAllDocuments();

    /**
     * Subscribe to changes for all documents that Id starts with given prefix.
     * @param docIdPrefix The document prefix
     * @return Changes observable
     */
    IChangesObservable<TChange> forDocumentsStartingWith(String docIdPrefix);

    /**
     * Subscribe to changes for all documents that belong to specified collection (Raven-Entity-Name).
     * @param collectionName The collection name.
     * @return Changes observable
     */
    IChangesObservable<TChange> forDocumentsInCollection(String collectionName);

    /**
     * Subscribe to changes for all documents that belong to specified collection (Raven-Entity-Name).
     * @param clazz The document class
     * @return Changes observable
     */
    IChangesObservable<TChange> forDocumentsInCollection(Class<?> clazz);
}
