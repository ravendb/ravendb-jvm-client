package net.ravendb.abstractions.replication;

import net.ravendb.abstractions.cluster.ClusterInformation;

public class ReplicationDestinationWithClusterInformation extends ReplicationDestination {
    private ClusterInformation clusterInformation;

    public ClusterInformation getClusterInformation() {
        return clusterInformation;
    }

    public void setClusterInformation(ClusterInformation clusterInformation) {
        this.clusterInformation = clusterInformation;
    }

}
