package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.documents.replication.ReplicationNode;

import java.time.Duration;

public class ExternalReplication extends ExternalReplicationBase implements IExternalReplication {

    private Duration delayReplicationFor;

    public ExternalReplication() {
    }

    public ExternalReplication(String database, String connectionStringName) {
        super(database, connectionStringName);
    }

    public Duration getDelayReplicationFor() {
        return delayReplicationFor;
    }

    public void setDelayReplicationFor(Duration delayReplicationFor) {
        this.delayReplicationFor = delayReplicationFor;
    }

    @Override
    public ReplicationType getReplicationType() {
        return ReplicationType.EXTERNAL;
    }

    @Override
    public boolean isEqualTo(ReplicationNode other) {
        if (other instanceof ExternalReplication) {
            ExternalReplication external = (ExternalReplication) other;

            return super.isEqualTo(other)
                    && delayReplicationFor == external.getDelayReplicationFor();
        }

        return false;
    }

}
