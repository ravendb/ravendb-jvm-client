package net.ravendb.client.documents.changes;

public interface IDatabaseChanges extends IDocumentChanges<DocumentChange>,
        IIndexChanges<IndexChange>,
        ICounterChanges<CounterChange>,
        ITimeSeriesChanges<TimeSeriesChange>,
        IConnectableChanges<IDatabaseChanges> {

    /**
     * Subscribe to changes for specified operation only.
     * @param operationId Operation id
     * @return Changes observable
     * @deprecated Operation changes are node-specific and are no longer part of IDatabaseChanges (matches the C# client).
     * This method throws IllegalArgumentException unless the changes were created with a node tag.
     * Use {@link ISingleNodeDatabaseChanges#forOperationId(long)} obtained via IDocumentStore.changes(database, nodeTag) instead.
     */
    @Deprecated
    IChangesObservable<OperationStatusChange> forOperationId(long operationId);

    /**
     * Subscribe to change for all operation statuses.
     * @return Changes observable
     * @deprecated Operation changes are node-specific and are no longer part of IDatabaseChanges (matches the C# client).
     * This method throws IllegalArgumentException unless the changes were created with a node tag.
     * Use {@link ISingleNodeDatabaseChanges#forAllOperations()} obtained via IDocumentStore.changes(database, nodeTag) instead.
     */
    @Deprecated
    IChangesObservable<OperationStatusChange> forAllOperations();
}
