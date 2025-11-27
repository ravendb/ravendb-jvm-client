package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.documents.replication.ReplicationNode;

public class PullReplicationAsHub extends ExternalReplication {

    private PullReplicationMode mode = PullReplicationMode.HUB_TO_SINK;

    public PullReplicationAsHub() {
        super();
    }

    public PullReplicationAsHub(String database, String connectionStringName) {
        super(database, connectionStringName);
    }

    @Override
    public ReplicationType getReplicationType() {
        return ReplicationType.PULL_AS_HUB;
    }

    @Override
    public boolean isEqualTo(ReplicationNode other) {
        if (other instanceof PullReplicationAsHub) {
            PullReplicationAsHub hub = (PullReplicationAsHub) other;
            return super.isEqualTo(other)
                    && getUrl().equalsIgnoreCase(hub.getUrl())
                    && getName().equalsIgnoreCase(hub.getName());
        }
        return false;
    }

    @Override
    public long getTaskKey() {
        long hashCode = super.getTaskKey();
        hashCode = (hashCode * 397) ^ calculateStringHash(getUrl());
        hashCode = (hashCode * 397) ^ mode.ordinal();
        return hashCode;
    }
}

