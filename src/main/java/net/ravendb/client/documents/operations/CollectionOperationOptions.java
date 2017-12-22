package net.ravendb.client.documents.operations;

public class CollectionOperationOptions {
    private Integer maxOpsPerSecond;

    public Integer getMaxOpsPerSecond() {
        return maxOpsPerSecond;
    }

    public void setMaxOpsPerSecond(Integer maxOpsPerSecond) {
        if (maxOpsPerSecond != null && maxOpsPerSecond < 0) {
            throw new IllegalArgumentException("MaxOpsPerSecond must be greater than 0");
        }
        this.maxOpsPerSecond = maxOpsPerSecond;
    }
}
