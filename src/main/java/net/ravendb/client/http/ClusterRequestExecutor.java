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

    protected ClusterRequestExecutor(KeyStore certificate, char[] keyPassword, KeyStore trustStore, DocumentConventions conventions, ExecutorService executorService, String[] initialUrls) {
        super(null, certificate, keyPassword, trustStore, conventions, executorService, initialUrls);
    }

    @SuppressWarnings("unused")
    public static ClusterRequestExecutor create(String[] urls, String databaseName, KeyStore certificate, char[] keyPassword, DocumentConventions conventions) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public static ClusterRequestExecutor createForSingleNodeWithConfigurationUpdates(String url, String databaseName, KeyStore certificate, char[] keyPassword, DocumentConventions conventions) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public static ClusterRequestExecutor createForSingleNodeWithoutConfigurationUpdates(String url, String databaseName, KeyStore certificate, char[] keyPassword, DocumentConventions conventions) {
        throw new UnsupportedOperationException();
    }

    public static ClusterRequestExecutor createForSingleNode(String url, KeyStore certificate, char[] keyPassword, KeyStore trustStore, ExecutorService executorService) {
        return createForSingleNode(url, certificate, keyPassword, trustStore, executorService, null);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static ClusterRequestExecutor createForSingleNode(String url, KeyStore certificate, char[] keyPassword, KeyStore trustStore, ExecutorService executorService, DocumentConventions conventions) {
        String[] initialUrls = {url};
        url = validateUrls(initialUrls, certificate)[0];

        ClusterRequestExecutor executor = new ClusterRequestExecutor(certificate, keyPassword, trustStore,
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

    public static ClusterRequestExecutor create(String[] initialUrls, KeyStore certificate, char[] keyPassword, KeyStore trustStore, ExecutorService executorService) {
        return create(initialUrls, certificate, keyPassword, trustStore, executorService, null);
    }

    public static ClusterRequestExecutor create(String[] initialUrls, KeyStore certificate, char[] keyPassword, KeyStore trustStore, ExecutorService executorService, DocumentConventions conventions) {
        ClusterRequestExecutor executor = new ClusterRequestExecutor(certificate, keyPassword, trustStore,
                conventions != null ? conventions : DocumentConventions.defaultConventions, executorService, initialUrls);

        executor._disableClientConfigurationUpdates = true;
        executor._firstTopologyUpdate = executor.firstTopologyUpdate(initialUrls, null);
        return executor;
    }

    @Override
    protected void performHealthCheck(ServerNode serverNode, int nodeIndex) {
        execute(serverNode, nodeIndex, new GetTcpInfoCommand("health-check"), false, null);
    }

    @Override
    public CompletableFuture<Boolean> updateTopologyAsync(UpdateTopologyParameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        if (_disposed) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean lockTaken = clusterTopologySemaphore.tryAcquire(parameters.getTimeoutInMs(), TimeUnit.MILLISECONDS);
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

                GetClusterTopologyCommand command = new GetClusterTopologyCommand(parameters.getDebugTag());
                execute(parameters.getNode(), null, command, false, null);

                ClusterTopologyResponse results = command.getResult();
                List<ServerNode> nodes = ServerNode.createFrom(results.getTopology());

                Topology newTopology = new Topology();
                newTopology.setNodes(nodes);
                newTopology.setEtag(results.getEtag());

                topologyEtag = results.getEtag();

                if (_nodeSelector == null) {
                    _nodeSelector = new NodeSelector(newTopology, _executorService);

                    if (getConventions().getReadBalanceBehavior() == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                } else if (_nodeSelector.onUpdateTopology(newTopology, parameters.isForceUpdate())) {
                    disposeAllFailedNodesTimers();

                    if (getConventions().getReadBalanceBehavior() == ReadBalanceBehavior.FASTEST_NODE) {
                        _nodeSelector.scheduleSpeedTest();
                    }
                }

                onTopologyUpdatedInvoke(newTopology);
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
    protected CompletableFuture<Void> updateClientConfigurationAsync(ServerNode serverNode) {
        return CompletableFuture.completedFuture(null);
    }

    protected void throwExceptions(String details) {
        throw new IllegalStateException("Failed to retrieve cluster topology from all known nodes" + System.lineSeparator() + details);
    }

}
