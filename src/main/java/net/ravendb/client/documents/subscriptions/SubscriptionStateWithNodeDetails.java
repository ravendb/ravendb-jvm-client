package net.ravendb.client.documents.subscriptions;

import net.ravendb.client.serverwide.operations.NodeId;

public class SubscriptionStateWithNodeDetails extends SubscriptionState {
    private NodeId responsibleNode;

    public NodeId getResponsibleNode() {
        return responsibleNode;
    }

    public void setResponsibleNode(NodeId responsibleNode) {
        this.responsibleNode = responsibleNode;
    }
}
