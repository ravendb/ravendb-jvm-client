package net.ravendb.client.http;

import java.util.List;

public class Topology {

    private Long etag;
    private List<ServerNode> nodes;
    private List<ServerNode> promotables;

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

    public List<ServerNode> getPromotables() {
        return promotables;
    }

    public void setPromotables(List<ServerNode> promotables) {
        this.promotables = promotables;
    }
}
