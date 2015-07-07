package net.ravendb.client.connection.request;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.closure.Action3;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.client.connection.IDocumentStoreReplicationInformer;
import net.ravendb.client.connection.OperationMetadata;
import net.ravendb.client.connection.ReplicationInformer;
import net.ravendb.client.connection.ServerClient;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.metrics.RequestTimeMetric;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.Map;

public class ReplicationAwareRequestExecuter implements IRequestExecuter {

    private final IDocumentStoreReplicationInformer replicationInformer;

    private final RequestTimeMetric requestTimeMetric;

    private int readStrippingBase;

    public ReplicationAwareRequestExecuter(IDocumentStoreReplicationInformer replicationInformer, RequestTimeMetric requestTimeMetric) {
        this.replicationInformer = replicationInformer;
        this.requestTimeMetric = requestTimeMetric;
    }

    public IDocumentStoreReplicationInformer getReplicationInformer() {
        return replicationInformer;
    }

    public int getReadStripingBase(boolean increment) {
        return readStrippingBase = replicationInformer.getReadStripingBase(increment);
    }

    public ReplicationDestination[] getFailoverServers() {
        return replicationInformer.getFailoverServers();
    }

    public void setFailoverServers(ReplicationDestination[] failoverServers) {
        replicationInformer.setFailoverServers(failoverServers);
    }

    @Override
    public <T> T executeOperation(ServerClient serverClient, HttpMethods method, int currentRequest, Function1<OperationMetadata, T> operation) {
        return replicationInformer.executeWithReplication(method, serverClient.getUrl(), serverClient.getPrimaryCredentials(), requestTimeMetric, currentRequest, readStrippingBase, operation);
    }

    @Override
    public void updateReplicationInformationIfNeeded(ServerClient serverClient) {
        updateReplicationInformationIfNeeded(serverClient, false);
    }

    @Override
    public void updateReplicationInformationIfNeeded(ServerClient serverClient, boolean force) {
        if (force) {
            throw new UnsupportedOperationException("Force is not supported in ReplicationAwareRequestExecuter");
        }

        replicationInformer.updateReplicationInformationIfNeeded(serverClient);
    }

    @Override
    public void addHeaders(HttpJsonRequest httpJsonRequest, ServerClient serverClient, final String currentUrl) {
        if (serverClient.getUrl().equals(currentUrl)) {
            return;
        }

        if (replicationInformer.getFailureCounters().getFailureCount(serverClient.getUrl()).get() <= 0) {
            return;// not because of failover, no need to do this.
        }

        Date lastPrimaryCheck = replicationInformer.getFailureCounters().getFailureLastCheck(serverClient.getUrl());
        httpJsonRequest.addOperationHeader(Constants.RAVEN_CLIENT_PRIMARY_SERVER_URL, toRemoteUrl(serverClient.getUrl()));
        httpJsonRequest.addOperationHeader(Constants.RAVEN_CLIENT_PRIMARY_SERVER_LAST_CHECK, lastPrimaryCheck.toString()); //TODO: format

        httpJsonRequest.addReplicationStatusChangeBehavior(serverClient.getUrl(), currentUrl, new Action3<Map<String, String>, String, String>() {
            @Override
            public void apply(Map<String, String> headers, String primaryUrl, String currentUrl) {
                handleReplicationStatusChanges(headers, primaryUrl, currentUrl);
            }
        });
    }

    private static String toRemoteUrl(String primaryUrl) {
        /* TODO
        var uriBuilder = new UriBuilder(primaryUrl);
+			if (uriBuilder.Host == "localhost" || uriBuilder.Host == "127.0.0.1")
+				uriBuilder.Host = Environment.MachineName;
+			return uriBuilder.Uri.ToString();
         */
        return primaryUrl;
    }

    @Override
    public CleanCloseable forceReadFromMaster() {
        final int old = this.readStrippingBase;
        readStrippingBase = -1; // this means that will have to use the master url first
        return new CleanCloseable() {
            @Override
            public void close() {
                readStrippingBase = old;
            }
        };
    }

    public void handleReplicationStatusChanges(Map<String, String> headers, String primaryUrl, String currentUrl) {
        if (primaryUrl.equals(currentUrl)) {
            return;
        }

        String forceCheck = headers.get(Constants.RAVEN_FORCE_PRIMARY_SERVER_CHECK);
        if (StringUtils.isNotEmpty(forceCheck)) {
            boolean forceCheckBool = Boolean.valueOf(forceCheck);
            replicationInformer.getFailureCounters().forceCheck(primaryUrl, forceCheckBool);
        }
    }

    @Override
    public void addFailoverStatusChanged(EventHandler<ReplicationInformer.FailoverStatusChangedEventArgs> event) {
        replicationInformer.addFailoverStatusChanged(event);
    }

    @Override
    public void removeFailoverStatusChanged(EventHandler<ReplicationInformer.FailoverStatusChangedEventArgs> event) {
        replicationInformer.removeFailoverStatusChanged(event);
    }
}
