package net.ravendb.client.http;

import java.util.UUID;

public class UpdateTopologyParameters {

    private final ServerNode node;
    private int timeoutInMs = 15_000;
    private boolean forceUpdate;
    private String debugTag;
    private UUID applicationIdentifier;

    public UpdateTopologyParameters(ServerNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node cannot be null");
        }
        this.node = node;
    }

    public ServerNode getNode() {
        return node;
    }

    public int getTimeoutInMs() {
        return timeoutInMs;
    }

    public void setTimeoutInMs(int timeoutInMs) {
        this.timeoutInMs = timeoutInMs;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
    }

    public String getDebugTag() {
        return debugTag;
    }

    public void setDebugTag(String debugTag) {
        this.debugTag = debugTag;
    }

    public UUID getApplicationIdentifier() {
        return applicationIdentifier;
    }

    public void setApplicationIdentifier(UUID applicationIdentifier) {
        this.applicationIdentifier = applicationIdentifier;
    }
}
