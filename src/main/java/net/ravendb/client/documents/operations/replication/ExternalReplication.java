package net.ravendb.client.documents.operations.replication;

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
}
