package net.ravendb.client.http;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.serverwide.commands.GetClusterTopologyCommand;
import net.ravendb.client.serverwide.commands.GetTcpInfoCommand;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ClusterRequestExecutor extends RequestExecutor {

    private final Semaphore clusterTopologySemaphone = new Semaphore(1);

    protected ClusterRequestExecutor(DocumentConventions conventions) {
        super(null, conventions);

        /* TODO certs
         // Here we are explicitly ignoring trust issues in the case of ClusterRequestExecutor.
            // this is because we don't actually require trust, we just use the certificate
            // as a way to authenticate. Either we encounter the same server certificate which we already
            // trust, or the admin is going to tell us which specific certs we can trust.
            ServerCertificateCustomValidationCallback += (msg, cert, chain, errors) => true;
         */
    }

    public static ClusterRequestExecutor create(String[] urls, String databaseName, DocumentConventions conventions) { //TODO: X509Certificate2 certificate
        throw new UnsupportedOperationException();
    }

    public static ClusterRequestExecutor createForSingleNodeWithConfigurationUpdates(String url, String databaseName, DocumentConventions conventions) { //TODO: X509Certificate2 certificate
        throw new UnsupportedOperationException();
    }

    public static ClusterRequestExecutor createForSingleNodeWithoutConfigurationUpdates(String url, String databaseName, DocumentConventions conventions) { //TODO: X509Certificate2 certificate
        throw new UnsupportedOperationException();
    }

    public static ClusterRequestExecutor createForSingleNode(String url) { //TODO: X509Certificate2 certificate
        url = validateUrls(new String[]{url})[0];

        ClusterRequestExecutor executor = new ClusterRequestExecutor(DocumentConventions.defaultConventions);

        ServerNode serverNode = new ServerNode();
        serverNode.setUrl(url);

        Topology topology = new Topology();
        topology.setEtag(-1L);
        topology.setNodes(Arrays.asList(serverNode));

        NodeSelector nodeSelector = new NodeSelector(topology);

        executor._nodeSelector = nodeSelector;
        executor.topologyEtag = -2L;
        executor._disableClientConfigurationUpdates = true;
        executor._disableTopologyUpdates = true;

        return executor;
    }

    public static ClusterRequestExecutor create(String[] urls) {
        return create(urls, null);
    }

    public static ClusterRequestExecutor create(String[] urls, DocumentConventions conventions) {
        ClusterRequestExecutor executor = new ClusterRequestExecutor(conventions != null ? conventions : DocumentConventions.defaultConventions);

        executor._disableClientConfigurationUpdates = true;
        executor._firstTopologyUpdate = executor.firstTopologyUpdate(urls);
        return executor;
    }

    @Override
    protected void performHealthCheck(ServerNode serverNode, int nodeIndex) {
        execute(serverNode, nodeIndex, new GetTcpInfoCommand("health-check"), false, null);
    }

    @Override
    public CompletableFuture<Boolean> updateTopologyAsync(ServerNode node, int timeout, boolean forceUpdate) {
        if (_disposed) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean lockTaken = clusterTopologySemaphone.tryAcquire(timeout, TimeUnit.MILLISECONDS);
                if (!lockTaken) {
                    return false;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {
                if (_disposed) {
                    return false;
                }

                GetClusterTopologyCommand command = new GetClusterTopologyCommand();
                execute(node, null, command, false, null);

                ClusterTopologyResponse results = command.getResult();
                List<ServerNode> nodes = results
                        .getTopology()
                        .getMembers()
                        .entrySet()
                        .stream()
                        .map(kvp -> {
                            ServerNode serverNode = new ServerNode();
                            serverNode.setUrl(kvp.getValue());
                            serverNode.setClusterTag(kvp.getKey());
                            return serverNode;
                        })
                        .collect(Collectors.toList());

                Topology newTopology = new Topology();
                newTopology.setNodes(nodes);

                if (_nodeSelector == null) {
                    _nodeSelector = new NodeSelector(newTopology);

                    if (_readBalanceBehavior == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                } else if (_nodeSelector.onUpdateTopology(newTopology, forceUpdate)) {
                    disposeAllFailedNodesTimers();

                    if (_readBalanceBehavior == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                }
            } finally {
                clusterTopologySemaphone.release();
            }

            return true;
        });
    }

    @Override
    protected CompletableFuture<Void> updateClientConfigurationAsync() {
        return CompletableFuture.completedFuture(null);
    }

    protected void throwExceptions(String details) {
        throw new IllegalStateException("Failed to retrieve cluster topology from all known nodes" + System.lineSeparator() + details);
    }

    @Override
    public void close() {
        try {
            clusterTopologySemaphone.acquire();
        } catch (InterruptedException e) {
        }

        super.close();
    }
}
