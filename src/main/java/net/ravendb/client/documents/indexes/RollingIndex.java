package net.ravendb.client.documents.indexes;

import java.util.Map;

public class RollingIndex {
    private Map<String, RollingIndexDeployment> activeDeployments;

    public Map<String, RollingIndexDeployment> getActiveDeployments() {
        return activeDeployments;
    }

    public void setActiveDeployments(Map<String, RollingIndexDeployment> activeDeployments) {
        this.activeDeployments = activeDeployments;
    }
}
