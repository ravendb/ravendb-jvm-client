package net.ravendb.client.serverwide.operations.documentsCompression;

public class DocumentCompressionConfigurationResult {
    private Long raftCommandIndex;

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
