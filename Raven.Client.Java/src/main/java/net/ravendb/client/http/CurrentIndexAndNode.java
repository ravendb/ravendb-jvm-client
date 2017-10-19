package net.ravendb.client.http;

public class CurrentIndexAndNode {
    public int currentIndex;
    public ServerNode currentNode;

    public CurrentIndexAndNode(int currentIndex, ServerNode currentNode) {
        this.currentIndex = currentIndex;
        this.currentNode = currentNode;
    }
}
