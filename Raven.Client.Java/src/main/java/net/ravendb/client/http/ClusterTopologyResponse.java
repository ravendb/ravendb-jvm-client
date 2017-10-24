package net.ravendb.client.http;

public class ClusterTopologyResponse {
    private String leader;
    private String nodeTag;
    private ClusterTopology topology;

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
}
