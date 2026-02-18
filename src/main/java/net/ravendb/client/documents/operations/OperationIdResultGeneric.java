package net.ravendb.client.documents.operations;

public final class OperationIdResultGeneric<TResult> extends OperationIdResult {

    private TResult result;

    public TResult getResult() {
        return result;
    }

    public void setResult(TResult result) {
        this.result = result;
    }
}

