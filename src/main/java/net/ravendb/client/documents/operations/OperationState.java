package net.ravendb.client.documents.operations;

public final class OperationState {
    private IOperationResult result;
    private IOperationProgress progress;
    private OperationStatus status;

    public IOperationResult getResult() {
        return result;
    }

    public void setResult(IOperationResult result) {
        this.result = result;
    }

    public IOperationProgress getProgress() {
        return progress;
    }

    public void setProgress(IOperationProgress progress) {
        this.progress = progress;
    }

    public OperationStatus getStatus() {
        return status;
    }

    public void setStatus(OperationStatus status) {
        this.status = status;
    }
}