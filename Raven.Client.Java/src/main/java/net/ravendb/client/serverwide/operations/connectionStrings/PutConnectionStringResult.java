package net.ravendb.client.serverwide.operations.connectionStrings;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PutConnectionStringResult {

    @JsonProperty("ETag")
    private Long etag;

    public Long getEtag() {
        return etag;
    }

    public void setEtag(Long etag) {
        this.etag = etag;
    }
}
