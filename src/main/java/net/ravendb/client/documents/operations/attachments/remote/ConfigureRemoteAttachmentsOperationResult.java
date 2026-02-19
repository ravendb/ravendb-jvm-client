package net.ravendb.client.documents.operations.attachments.remote;

/**
 * Represents the result of a remote attachments configuration operation.
 * <p>
 * This result contains information about the execution of the underlying
 * Raft command.
 * </p>
 */
public final class ConfigureRemoteAttachmentsOperationResult {
    /**
     * Gets or sets the index of the Raft command that was executed to configure
     * remote attachments.
     * <p>
     * This value is {@code null} if the operation was not executed through the
     * Raft consensus mechanism.
     * </p>
     */
    private Long raftCommandIndex;

    public Long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(Long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
