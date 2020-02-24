package net.ravendb.client.http;

public class CurrentIndexAndNodeAndEtag {
    public final int currentIndex;
    public final ServerNode currentNode;
    public final long topologyEtag;

    public CurrentIndexAndNodeAndEtag(int currentIndex, ServerNode currentNode, long topologyEtag) {
        this.currentIndex = currentIndex;
        this.currentNode = currentNode;
        this.topologyEtag = topologyEtag;
    }
}
