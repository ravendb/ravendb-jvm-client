package net.ravendb.client.documents.changes;

/**
 * Changes API bound to a single node (obtained via IDocumentStore.changes(database, nodeTag)).
 * Unlike {@link IDatabaseChanges} it allows tracking node-specific operations.
 * <p>
 * Note: in Java this interface extends {@link IDatabaseChanges} (in C# they are siblings), because a class cannot
 * implement IConnectableChanges with two different type arguments.
 */
public interface ISingleNodeDatabaseChanges extends IDatabaseChanges, IOperationChanges<OperationStatusChange> {

    @Override
    ISingleNodeDatabaseChanges ensureConnectedNow();

    @Override
    IChangesObservable<OperationStatusChange> forOperationId(long operationId);

    @Override
    IChangesObservable<OperationStatusChange> forAllOperations();
}
