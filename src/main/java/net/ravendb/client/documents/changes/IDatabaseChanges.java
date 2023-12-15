package net.ravendb.client.documents.changes;

public interface IDatabaseChanges extends IDocumentChanges<DocumentChange>,
        IIndexChanges<IndexChange>,
        IOperationChanges<OperationStatusChange>,
        ICounterChanges<CounterChange>,
        ITimeSeriesChanges<TimeSeriesChange>,
        IConnectableChanges<IDatabaseChanges> {
}
