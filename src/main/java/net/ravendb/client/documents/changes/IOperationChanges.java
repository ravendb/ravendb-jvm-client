package net.ravendb.client.documents.changes;

public interface IOperationChanges<TChange> {

    /**
     * Subscribe to changes for specified operation only.
     * @param operationId Operation id
     * @return Changes observable
     */
    IChangesObservable<TChange> forOperationId(long operationId);

    /**
     * Subscribe to change for all operation statuses.
     * @return Changes observable
     */
    IChangesObservable<TChange> forAllOperations();

}
