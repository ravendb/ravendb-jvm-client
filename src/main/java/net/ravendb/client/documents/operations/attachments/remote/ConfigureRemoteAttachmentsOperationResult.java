package net.ravendb.client.documents.operations.attachments.remote;

public final class ConfigureRemoteAttachmentsOperationResult {

    private Long raftCommandIndex;

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
