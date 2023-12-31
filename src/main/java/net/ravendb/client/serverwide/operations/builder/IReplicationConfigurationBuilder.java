package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.documents.operations.replication.ExternalReplication;
import net.ravendb.client.documents.operations.replication.PullReplicationAsSink;
import net.ravendb.client.documents.operations.replication.PullReplicationDefinition;

public interface IReplicationConfigurationBuilder {
    IReplicationConfigurationBuilder addExternalReplication(ExternalReplication configuration);
    IReplicationConfigurationBuilder addPullReplicationSink(PullReplicationAsSink configuration);
    IReplicationConfigurationBuilder addPullReplicationHub(PullReplicationDefinition configuration);
}
