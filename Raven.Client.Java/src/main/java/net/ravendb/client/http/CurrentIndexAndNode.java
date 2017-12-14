package net.ravendb.client.http;

public class CurrentIndexAndNode {
    public final int currentIndex;
    public final ServerNode currentNode;

    public CurrentIndexAndNode(int currentIndex, ServerNode currentNode) {
        this.currentIndex = currentIndex;
        this.currentNode = currentNode;
    }
}
