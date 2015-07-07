package net.ravendb.raft;

public class ClusterConfiguration {
    private boolean enableReplication = true;

    public boolean isEnableReplication() {
        return enableReplication;
    }

    public void setEnableReplication(boolean enableReplication) {
        this.enableReplication = enableReplication;
    }
}
