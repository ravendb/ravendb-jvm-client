package net.ravendb.client.serverwide.operations.connectionStrings;

/**
 * The result of a {@link RemoveServerWideConnectionStringOperation}.
 */
public class RemoveServerWideConnectionStringResult {

    private long raftCommandIndex;

    /**
     * @return the Raft command index assigned to this operation. Can be used to wait for the operation to
     *         be applied across the cluster.
     */
    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
