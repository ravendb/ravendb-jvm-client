package net.ravendb.client.http;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.serverwide.commands.GetClusterTopologyCommand;
import net.ravendb.client.serverwide.commands.GetTcpInfoCommand;
import org.apache.commons.lang3.ObjectUtils;

import java.security.KeyStore;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ClusterRequestExecutor extends RequestExecutor {

    private final Semaphore clusterTopologySemaphore = new Semaphore(1);

    protected ClusterRequestExecutor(KeyStore certificate, KeyStore trustStore, DocumentConventions conventions, ExecutorService executorService, String[] initialUrls) {
        super(null, certificate, trustStore, conventions, executorService, initialUrls);
    }

    @SuppressWarnings("unused")
    public static ClusterRequestExecutor create(String[] urls, String databaseName, KeyStore certificate, DocumentConventions conventions) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public static ClusterRequestExecutor createForSingleNodeWithConfigurationUpdates(String url, String databaseName, KeyStore certificate, DocumentConventions conventions) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public static ClusterRequestExecutor createForSingleNodeWithoutConfigurationUpdates(String url, String databaseName, KeyStore certificate, DocumentConventions conventions) {
        throw new UnsupportedOperationException();
    }

    public static ClusterRequestExecutor createForSingleNode(String url, KeyStore certificate, KeyStore trustStore, ExecutorService executorService) {
        return createForSingleNode(url, certificate, trustStore, executorService, null);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static ClusterRequestExecutor createForSingleNode(String url, KeyStore certificate, KeyStore trustStore, ExecutorService executorService, DocumentConventions conventions) {
        String[] initialUrls = {url};
        url = validateUrls(initialUrls, certificate)[0];

        ClusterRequestExecutor executor = new ClusterRequestExecutor(certificate, trustStore,
                ObjectUtils.firstNonNull(conventions, DocumentConventions.defaultConventions), executorService, initialUrls);

        ServerNode serverNode = new ServerNode();
        serverNode.setUrl(url);

        Topology topology = new Topology();
        topology.setEtag(-1L);
        topology.setNodes(Collections.singletonList(serverNode));

        NodeSelector nodeSelector = new NodeSelector(topology, executorService);

        executor._nodeSelector = nodeSelector;
        executor.topologyEtag = -2L;
        executor._disableClientConfigurationUpdates = true;
        executor._disableTopologyUpdates = true;

        return executor;
    }

    public static ClusterRequestExecutor create(String[] initialUrls, KeyStore certificate, KeyStore trustStore, ExecutorService executorService) {
        return create(initialUrls, certificate, trustStore, executorService, null);
    }

    public static ClusterRequestExecutor create(String[] initialUrls, KeyStore certificate, KeyStore trustStore, ExecutorService executorService, DocumentConventions conventions) {
        ClusterRequestExecutor executor = new ClusterRequestExecutor(certificate, trustStore,
                conventions != null ? conventions : DocumentConventions.defaultConventions, executorService, initialUrls);

        executor._disableClientConfigurationUpdates = true;
        executor._firstTopologyUpdate = executor.firstTopologyUpdate(initialUrls);
        return executor;
    }

    @Override
    protected void performHealthCheck(ServerNode serverNode, int nodeIndex) {
        execute(serverNode, nodeIndex, new GetTcpInfoCommand("health-check"), false, null);
    }

    public CompletableFuture<Boolean> updateTopologyAsync(ServerNode node, int timeout) {
        return updateTopologyAsync(node, timeout, false, null);
    }

    public CompletableFuture<Boolean> updateTopologyAsync(ServerNode node, int timeout, boolean forceUpdate) {
        return updateTopologyAsync(node, timeout, forceUpdate, null);
    }

    @Override
    public CompletableFuture<Boolean> updateTopologyAsync(ServerNode node, int timeout, boolean forceUpdate, String debugTag) {
        if (_disposed) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean lockTaken = clusterTopologySemaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS);
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

                GetClusterTopologyCommand command = new GetClusterTopologyCommand(debugTag);
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
                    _nodeSelector = new NodeSelector(newTopology, _executorService);

                    if (_readBalanceBehavior == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                } else if (_nodeSelector.onUpdateTopology(newTopology, forceUpdate)) {
                    disposeAllFailedNodesTimers();

                    if (_readBalanceBehavior == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                }
            } catch (Exception e) {
                if (!_disposed) {
                    throw e;
                }
            } finally {
                clusterTopologySemaphore.release();
            }

            return true;
        }, _executorService);
    }

    @Override
    protected CompletableFuture<Void> updateClientConfigurationAsync() {
        return CompletableFuture.completedFuture(null);
    }

    protected void throwExceptions(String details) {
        throw new IllegalStateException("Failed to retrieve cluster topology from all known nodes" + System.lineSeparator() + details);
    }

}
