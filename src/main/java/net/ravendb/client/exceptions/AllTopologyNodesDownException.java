package net.ravendb.client.exceptions;

import net.ravendb.client.http.Topology;

public class AllTopologyNodesDownException extends RuntimeException {

    private Topology failedTopology;

    public Topology getFailedTopology() {
        return failedTopology;
    }

    public AllTopologyNodesDownException() {
    }

    public AllTopologyNodesDownException(String message) {
        super(message);
    }

    public AllTopologyNodesDownException(String message, Throwable cause) {
        super(message, cause);
    }

    public AllTopologyNodesDownException(String message, Topology failedTopology, Throwable cause) {
        super(message, cause);
        this.failedTopology = failedTopology;
    }
}
