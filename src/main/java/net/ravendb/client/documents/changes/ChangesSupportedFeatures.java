package net.ravendb.client.documents.changes;

public class ChangesSupportedFeatures {
    private boolean topologyChange;
    private boolean aggressiveCachingChange;

    public boolean isTopologyChange() {
        return topologyChange;
    }

    public void setTopologyChange(boolean topologyChange) {
        this.topologyChange = topologyChange;
    }

    public boolean isAggressiveCachingChange() {
        return aggressiveCachingChange;
    }

    public void setAggressiveCachingChange(boolean aggressiveCachingChange) {
        this.aggressiveCachingChange = aggressiveCachingChange;
    }
}
