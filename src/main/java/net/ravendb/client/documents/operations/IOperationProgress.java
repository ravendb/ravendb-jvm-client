package net.ravendb.client.documents.operations;

public interface IOperationProgress {

    IOperationProgress clone();

    boolean canMerge();

    void mergeWith(IOperationProgress progress);
}