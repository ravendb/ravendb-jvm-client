package net.ravendb.client.documents.operations.timeSeries;

public class ConfigureTimeSeriesOperationResult {
    private Long raftCommandIndex;

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
