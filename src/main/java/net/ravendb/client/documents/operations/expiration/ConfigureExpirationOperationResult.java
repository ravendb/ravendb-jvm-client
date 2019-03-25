package net.ravendb.client.documents.operations.expiration;

public class ConfigureExpirationOperationResult {

    private Long raftCommandIndex;

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
