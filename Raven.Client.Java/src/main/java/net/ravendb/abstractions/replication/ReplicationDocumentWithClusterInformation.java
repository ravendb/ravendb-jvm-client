package net.ravendb.abstractions.replication;

import net.ravendb.abstractions.cluster.ClusterInformation;

public class ReplicationDocumentWithClusterInformation extends ReplicationDocumentWithGeneric<ReplicationDestinationWithClusterInformation> {

    private ClusterInformation clusterInformation;
    private long clusterCommitIndex;

    public ReplicationDocumentWithClusterInformation() {
        clusterInformation = ClusterInformation.NOT_IN_CLUSTER;
        clusterCommitIndex = -1;
    }

    public long getClusterCommitIndex() {
        return clusterCommitIndex;
    }

    public void setClusterCommitIndex(long clusterCommitIndex) {
        this.clusterCommitIndex = clusterCommitIndex;
    }

    public ClusterInformation getClusterInformation() {
        return clusterInformation;
    }

    public void setClusterInformation(ClusterInformation clusterInformation) {
        this.clusterInformation = clusterInformation;
    }
}
