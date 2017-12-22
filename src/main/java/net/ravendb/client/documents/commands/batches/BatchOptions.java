package net.ravendb.client.documents.commands.batches;

import java.time.Duration;

public class BatchOptions {

    private boolean waitForReplicas;
    private int numberOfReplicasToWaitFor;
    private Duration waitForReplicasTimeout;
    private boolean majority;
    private boolean throwOnTimeoutInWaitForReplicas;

    private boolean waitForIndexes;
    private Duration waitForIndexesTimeout;
    private boolean throwOnTimeoutInWaitForIndexes;
    private String[] waitForSpecificIndexes;

    public boolean isWaitForReplicas() {
        return waitForReplicas;
    }

    public void setWaitForReplicas(boolean waitForReplicas) {
        this.waitForReplicas = waitForReplicas;
    }

    public int getNumberOfReplicasToWaitFor() {
        return numberOfReplicasToWaitFor;
    }

    public void setNumberOfReplicasToWaitFor(int numberOfReplicasToWaitFor) {
        this.numberOfReplicasToWaitFor = numberOfReplicasToWaitFor;
    }

    public Duration getWaitForReplicasTimeout() {
        return waitForReplicasTimeout;
    }

    public void setWaitForReplicasTimeout(Duration waitForReplicasTimeout) {
        this.waitForReplicasTimeout = waitForReplicasTimeout;
    }

    public boolean isMajority() {
        return majority;
    }

    public void setMajority(boolean majority) {
        this.majority = majority;
    }

    public boolean isThrowOnTimeoutInWaitForReplicas() {
        return throwOnTimeoutInWaitForReplicas;
    }

    public void setThrowOnTimeoutInWaitForReplicas(boolean throwOnTimeoutInWaitForReplicas) {
        this.throwOnTimeoutInWaitForReplicas = throwOnTimeoutInWaitForReplicas;
    }

    public boolean isWaitForIndexes() {
        return waitForIndexes;
    }

    public void setWaitForIndexes(boolean waitForIndexes) {
        this.waitForIndexes = waitForIndexes;
    }

    public Duration getWaitForIndexesTimeout() {
        return waitForIndexesTimeout;
    }

    public void setWaitForIndexesTimeout(Duration waitForIndexesTimeout) {
        this.waitForIndexesTimeout = waitForIndexesTimeout;
    }

    public boolean isThrowOnTimeoutInWaitForIndexes() {
        return throwOnTimeoutInWaitForIndexes;
    }

    public void setThrowOnTimeoutInWaitForIndexes(boolean throwOnTimeoutInWaitForIndexes) {
        this.throwOnTimeoutInWaitForIndexes = throwOnTimeoutInWaitForIndexes;
    }

    public String[] getWaitForSpecificIndexes() {
        return waitForSpecificIndexes;
    }

    public void setWaitForSpecificIndexes(String[] waitForSpecificIndexes) {
        this.waitForSpecificIndexes = waitForSpecificIndexes;
    }
}
