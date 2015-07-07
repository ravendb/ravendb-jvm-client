package net.ravendb.client.connection.request;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.client.connection.OperationMetadata;
import net.ravendb.client.connection.ReplicationInformer;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.connection.implementation.HttpJsonRequest;

public interface IRequestExecuter {
    int getReadStripingBase(boolean increment);

    ReplicationDestination[] getFailoverServers();

    void setFailoverServers(ReplicationDestination[] destinations);

    <T> T executeOperation(ServerClient serverClient, HttpMethods method, int currentRequest, Function1<OperationMetadata, T> operation);

    void updateReplicationInformationIfNeeded(ServerClient serverClient);

    void updateReplicationInformationIfNeeded(ServerClient serverClient, boolean force);

    CleanCloseable forceReadFromMaster();

    void addFailoverStatusChanged(EventHandler<ReplicationInformer.FailoverStatusChangedEventArgs> event);

    void removeFailoverStatusChanged(EventHandler<ReplicationInformer.FailoverStatusChangedEventArgs> event);

    void addHeaders(HttpJsonRequest httpJsonRequest, ServerClient serverClient, String currentUrl);

}
