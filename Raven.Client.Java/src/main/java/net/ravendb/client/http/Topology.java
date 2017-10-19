package net.ravendb.client.http;

import java.util.List;

public class Topology {

    private Long etag;
    private List<ServerNode> nodes;

    public Long getEtag() {
        return etag;
    }

    public void setEtag(Long etag) {
        this.etag = etag;
    }

    public List<ServerNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<ServerNode> nodes) {
        this.nodes = nodes;
    }
}
