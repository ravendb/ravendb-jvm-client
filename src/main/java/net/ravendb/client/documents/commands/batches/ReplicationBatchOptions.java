package net.ravendb.client.documents.commands.batches;

import java.time.Duration;

public class ReplicationBatchOptions {

    private boolean waitForReplicas;
    private int numberOfReplicasToWaitFor;
    private Duration waitForReplicasTimeout;
    private boolean majority;
    private boolean throwOnTimeoutInWaitForReplicas = true;

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
}
