package net.ravendb.client.http;

public class CurrentIndexAndNode {
    public final Integer currentIndex;
    public final ServerNode currentNode;

    public CurrentIndexAndNode(Integer currentIndex, ServerNode currentNode) {
        this.currentIndex = currentIndex;
        this.currentNode = currentNode;
    }
}
