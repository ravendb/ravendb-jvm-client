package net.ravendb.client.documents.operations.connectionStrings;

public class RemoveConnectionStringResult {
    private Long eTag;
    private long raftCommandIndex;

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }

    /**
     * ETag is not supported anymore. Will be removed in next major version of the product. Please use RaftCommandIndex instead.
     * @return etag
     */
    public Long geteTag() {
        return eTag;
    }

    public void seteTag(Long eTag) {
        this.eTag = eTag;
    }
}
