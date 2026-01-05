package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.documents.replication.ReplicationNode;
import org.apache.commons.lang3.StringUtils;

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
                    && StringUtils.equalsIgnoreCase(getUrl(), hub.getUrl())
                    && StringUtils.equalsIgnoreCase(getName(), hub.getName());
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

