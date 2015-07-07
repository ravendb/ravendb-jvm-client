package net.ravendb.abstractions.cluster;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ClusterInformation {

    public final static ClusterInformation NOT_IN_CLUSTER = new ClusterInformation(false, false);

    private boolean inCluster;
    private boolean isLeader;

    public ClusterInformation() {
        // empty by design
    }

    public ClusterInformation(boolean isInCluster, boolean isLeader) {
        this.inCluster = isInCluster;
        this.isLeader = isLeader;
    }

    public boolean isInCluster() {
        return inCluster;
    }

    public void setInCluster(boolean inCluster) {
        this.inCluster = inCluster;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void setIsLeader(boolean isLeader) {
        this.isLeader = isLeader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ClusterInformation that = (ClusterInformation) o;

        return new EqualsBuilder()
                .append(inCluster, that.inCluster)
                .append(isLeader, that.isLeader)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(inCluster)
                .append(isLeader)
                .toHashCode();
    }
}
