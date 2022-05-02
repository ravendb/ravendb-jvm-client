package net.ravendb.client.documents.indexes;

import java.util.Map;

public class RollingIndex {
    private Map<String, RollingIndexDeployment> activeDeployments;

    private long raftCommandIndex;

    public Map<String, RollingIndexDeployment> getActiveDeployments() {
        return activeDeployments;
    }

    public void setActiveDeployments(Map<String, RollingIndexDeployment> activeDeployments) {
        this.activeDeployments = activeDeployments;
    }

    public long getRaftCommandIndex() {
        return raftCommandIndex;
    }

    public void setRaftCommandIndex(long raftCommandIndex) {
        this.raftCommandIndex = raftCommandIndex;
    }
}
