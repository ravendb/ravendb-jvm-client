package net.ravendb.client.serverwide.operations;

public class NodeId {
    private String nodeTag;
    private String nodeUrl;
    private String responsibleNode;

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }

    public String getNodeUrl() {
        return nodeUrl;
    }

    public void setNodeUrl(String nodeUrl) {
        this.nodeUrl = nodeUrl;
    }

    public String getResponsibleNode() {
        return responsibleNode;
    }

    public void setResponsibleNode(String responsibleNode) {
        this.responsibleNode = responsibleNode;
    }
}
