package net.ravendb.client.documents.operations.connectionStrings;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PutConnectionStringResult {

    @JsonProperty("ETag")
    private Long etag;

    private long raftCommandIndex;

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }

    /**
     * @deprecated ETag is not supported anymore. Will be removed in next major version of the product. Please use raftCommandIndex instead
     * @return etag
     */
    public Long getEtag() {
        return etag;
    }

    public void setEtag(Long etag) {
        this.etag = etag;
    }
}
