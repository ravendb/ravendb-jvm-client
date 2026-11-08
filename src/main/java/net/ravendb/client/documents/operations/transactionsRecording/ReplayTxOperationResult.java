package net.ravendb.client.documents.operations.transactionsRecording;

import net.ravendb.client.documents.operations.IOperationResult;

public final class ReplayTxOperationResult implements IOperationResult {

    private long executedCommandsAmount;

    public long getExecutedCommandsAmount() {
        return executedCommandsAmount;
    }

    public void setExecutedCommandsAmount(long executedCommandsAmount) {
        this.executedCommandsAmount = executedCommandsAmount;
    }

    @Override
    public String getMessage() {
        return "Processed " + executedCommandsAmount + " commands.";
    }

    public boolean isShouldPersist() {
        return false;
    }
}
