package net.ravendb.client.http;

import java.util.Map;

public class ClusterTopologyResponse {
    private String leader;
    private String nodeTag;
    private ClusterTopology topology;
    private long etag;
    private Map<String, NodeStatus> status;

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }

    public ClusterTopology getTopology() {
        return topology;
    }

    public void setTopology(ClusterTopology topology) {
        this.topology = topology;
    }

    public Map<String, NodeStatus> getStatus() {
        return status;
    }

    public void setStatus(Map<String, NodeStatus> status) {
        this.status = status;
    }

    public long getEtag() {
        return etag;
    }

    public void setEtag(long etag) {
        this.etag = etag;
    }
}
