package net.ravendb.client.connection.request;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.cluster.ClusterBehavior;
import net.ravendb.abstractions.cluster.ClusterInformation;
import net.ravendb.abstractions.connection.ErrorResponseException;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.replication.ReplicationDestination;
import net.ravendb.abstractions.replication.ReplicationDestinationWithClusterInformation;
import net.ravendb.abstractions.replication.ReplicationDocumentWithClusterInformation;
import net.ravendb.abstractions.util.ManualResetEvent;
import net.ravendb.abstractions.util.TimeUtils;
import net.ravendb.client.connection.*;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.extensions.MultiDatabase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpStatus;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterAwareRequestExecuter implements IRequestExecuter {

    private final static int WAIT_FOR_LEADER_TIMEOUT_IN_SECONDS = 30;
    private final static int GET_REPLICATION_DESTINATIONS_TIMEOUT_IN_SECONDS = 2;
    private final ManualResetEvent leaderNodeSelected = new ManualResetEvent(false);

    private Thread refreshReplicationInformationTask;

    private OperationMetadata leaderNode;

    private Date lastUpdate = new Date(0);

    private boolean firstTime = true;

    private AtomicInteger readStripingBase = new AtomicInteger(0);

    private List<OperationMetadata> nodes;

    private FailureCounters failureCounters;

    private ReplicationDestination[] failoverServers;

    public OperationMetadata getLeaderNode() {
        return leaderNode;
    }

    public void setLeaderNode(OperationMetadata leaderNode) {
        if (leaderNode == null) {
            leaderNodeSelected.reset();
            this.leaderNode = null;
            return;
        }
        this.leaderNode = leaderNode;
        leaderNodeSelected.set();
    }

    public List<OperationMetadata> getNodes() {
        return nodes;
    }

    public List<OperationMetadata> getNodeUrls() {
        List<OperationMetadata> result = new ArrayList<>();
        for (OperationMetadata node : nodes) {
            result.add(new OperationMetadata(node));
        }
        return result;
    }

    public FailureCounters getFailureCounters() {
        return failureCounters;
    }

    public ClusterAwareRequestExecuter() {
        nodes = new ArrayList<>();
        failureCounters = new FailureCounters();
    }

    @Override
    public int getReadStripingBase(boolean increment) {
        return increment? readStripingBase.incrementAndGet() : readStripingBase.get();
    }

    @Override
    public ReplicationDestination[] getFailoverServers() {
        return failoverServers;
    }

    @Override
    public void setFailoverServers(ReplicationDestination[] failoverServers) {
        this.failoverServers = failoverServers;
    }

    @Override
    public <T> T executeOperation(ServerClient serverClient, HttpMethods method, int currentRequest, Function1<OperationMetadata, T> operation) {
        return executeWithinClusterInternal(serverClient, method, operation);
    }

    @Override
    public void updateReplicationInformationIfNeeded(ServerClient serverClient) {
        updateReplicationInformationIfNeeded(serverClient, false);
    }

    @Override
    public void updateReplicationInformationIfNeeded(final ServerClient serverClient, boolean force) {
        if (!force && DateUtils.addMinutes(lastUpdate,5).getTime() > new Date().getTime() && getLeaderNode() != null) {
            return;
        }

        setLeaderNode(null);
        updateReplicationInformationForCluster(new OperationMetadata(serverClient.getUrl(), serverClient.getPrimaryCredentials(), null), new Function1<OperationMetadata, ReplicationDocumentWithClusterInformation>() {
            @Override
            public ReplicationDocumentWithClusterInformation apply(OperationMetadata operationMetadata) {
                return serverClient.directGetReplicationDestinations(operationMetadata);
            }
        });
    }

    public void addHeaders(HttpJsonRequest httpJsonRequest, ServerClient serverClient, String currentUrl) {
        httpJsonRequest.addOperationHeader(Constants.Cluster.CLUSTER_AWARE_HEADER, "true");

        if (serverClient.getClusterBehavior() == ClusterBehavior.NONE.READ_FROM_ALL_WRITE_TO_LEADER) {
            httpJsonRequest.addOperationHeader(Constants.Cluster.CLUSTER_READ_BEHAVIOR_HEADER, "All");
        }

        if (serverClient.getClusterBehavior() == ClusterBehavior.READ_FROM_ALL_WRITE_TO_LEADER_WITH_FAILOVERS || serverClient.getClusterBehavior() == ClusterBehavior.READ_FROM_LEADER_WRITE_TO_LEADER_WITH_FAILOVERS) {
            httpJsonRequest.addOperationHeader(Constants.Cluster.CLUSTER_FAILOVER_BEHAVIOR_HEADER, "true");
        }
    }

    private <T> T executeWithinClusterInternal(ServerClient serverClient, HttpMethods method, Function1<OperationMetadata, T> operation) {
        return executeWithinClusterInternal(serverClient, method, operation, 2);
    }

    private <T> T executeWithinClusterInternal(ServerClient serverClient, HttpMethods method, Function1<OperationMetadata, T> operation, int numberOfRetires) {
        try {
            if (numberOfRetires < 0) {
                throw new IllegalStateException("Cluster is not reachable. Out of retires, aborting.");
            }

            OperationMetadata node = getLeaderNode();
            if (node == null) {
                updateReplicationInformationIfNeeded(serverClient);

                switch (serverClient.getClusterBehavior()) {
                    case READ_FROM_ALL_WRITE_TO_LEADER_WITH_FAILOVERS:
                    case READ_FROM_LEADER_WRITE_TO_LEADER_WITH_FAILOVERS:
                        if (nodes.isEmpty()) {
                            leaderNodeSelected.waitOne(WAIT_FOR_LEADER_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
                        }
                        break;
                    default:
                        if (!leaderNodeSelected.waitOne(WAIT_FOR_LEADER_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Cluster is not reachable. No leader was selected, aborting.");
                        }
                        break;
                }

                node = getLeaderNode();
            }

            switch (serverClient.getClusterBehavior()) {
                case READ_FROM_ALL_WRITE_TO_LEADER:
                    if (HttpMethods.GET == method) {
                        node = getNodeForReadOperation(node);
                    }
                    break;
                case READ_FROM_ALL_WRITE_TO_LEADER_WITH_FAILOVERS:
                    if (node == null) {
                        return handleWithFailovers(operation);
                    }

                    if (method == HttpMethods.GET) {
                        node = getNodeForReadOperation(node);
                    }
                    break;
                case READ_FROM_LEADER_WRITE_TO_LEADER_WITH_FAILOVERS:
                    if (node == null) {
                        return handleWithFailovers(operation);
                    }
                    break;
            }

            ReplicationInformerBase.OperationResult<T> operationResult = tryClusterOperation(node, operation, false);
            if (operationResult.isSuccess()) {
                return operationResult.getResult();
            }

            setLeaderNode(null);
            failureCounters.incrementFailureCount(node.getUrl());
            return executeWithinClusterInternal(serverClient, method, operation, numberOfRetires - 1);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private OperationMetadata getNodeForReadOperation(OperationMetadata node) {
        Objects.requireNonNull(node);

        List<OperationMetadata> nodes = getNodeUrls();

        int nodeIndex = readStripingBase.get() % nodes.size();
        OperationMetadata readNode = nodes.get(nodeIndex);
        if (shouldExecuteUsing(readNode)) {
            return readNode;
        }
        return node;
    }

    private <T> T handleWithFailovers(Function1<OperationMetadata, T> operation) {
        List<OperationMetadata> nodes = getNodeUrls();
        for (int i = 0; i < nodes.size(); i++) {
            OperationMetadata n = nodes.get(i);
            if (!shouldExecuteUsing(n)) {
                continue;
            }

            boolean hasMoreNodes = nodes.size() > i + 1;
            ReplicationInformerBase.OperationResult<T> result = tryClusterOperation(n, operation, hasMoreNodes);
            if (result.isSuccess()) {
                return result.getResult();
            }

            failureCounters.incrementFailureCount(n.getUrl());
        }

        throw new IllegalStateException("Cluster is not reachable. Executing operation on any of the nodes failed, aborting.");
    }

    private boolean shouldExecuteUsing(OperationMetadata operationMetadata) {
        FailureCounters.FailureCounter failureCounter = getFailureCounters().getHolder(operationMetadata.getUrl());
        if (failureCounter.getValue().get() <= 1L) { //can fail once
            return true;
        }
        return false;
    }

    private <T> ReplicationInformerBase.OperationResult<T> tryClusterOperation(OperationMetadata node, Function1<OperationMetadata, T> operation, boolean avoidThrowing) {
        Objects.requireNonNull(node);
        boolean shouldRetry = false;

        ReplicationInformerBase.OperationResult<T> operationResult = new ReplicationInformerBase.OperationResult<>();
        try {
            operationResult.setResult(operation.apply(node));
            operationResult.setSuccess(true);
        } catch (Exception e) {
            Reference<Boolean> wasTimeout = new Reference<>();
            if (HttpConnectionHelper.isServerDown(e, wasTimeout)) {
                shouldRetry = true;
                operationResult.setWasTimeout(wasTimeout.value);
            } else {
                if (e instanceof ErrorResponseException) {
                    ErrorResponseException ere = (ErrorResponseException) e;
                    if (ere.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY || ere.getStatusCode() == HttpStatus.SC_EXPECTATION_FAILED) {
                        shouldRetry = true;
                    }
                }
            }

            if (!shouldRetry && !avoidThrowing) {
                throw e;
            }
        }

        if (operationResult.isSuccess()) {
            failureCounters.resetFailureCount(node.getUrl());
        }

        return operationResult;
    }

    private void updateReplicationInformationForCluster(final OperationMetadata primaryNode, final Function1<OperationMetadata, ReplicationDocumentWithClusterInformation> getReplicationDestionations) {
        synchronized (this) {
            final String serverHash = ServerHash.getServerHash(primaryNode.getUrl());

            Thread taskCopy = this.refreshReplicationInformationTask;
            if (taskCopy != null) {
                return;
            }

            if (firstTime) {
                firstTime = false;

                List<OperationMetadata> nodes = ReplicationInformerLocalCache.tryLoadClusterNodesFromLocalCache(serverHash);
                if (nodes != null) {
                    this.nodes = nodes;
                    this.setLeaderNode(getLeaderNode(nodes));

                    if (getLeaderNode() != null) {
                        return;
                    }
                }
            }

            Runnable refreshTask = new Runnable() {
                @Override
                public void run() {
                    boolean tryFailoverServer = false;
                    boolean triedFailoverServer = failoverServers == null || failoverServers.length == 0;
                    while (true) {
                        try {
                            Set<OperationMetadata> nodes = new HashSet<>(getNodeUrls());

                            if (!tryFailoverServer) {
                                if (nodes.isEmpty()) {
                                    nodes.add(primaryNode);
                                }
                            } else {
                                nodes.add(primaryNode); // always check primary node during failover check
                                for (ReplicationDestination failoverServer : failoverServers) {
                                    OperationMetadata node = convertReplicationDestinationToOperationMetadata(failoverServer, ClusterInformation.NOT_IN_CLUSTER);
                                    if (node != null) {
                                        nodes.add(node);
                                    }
                                }

                                triedFailoverServer = true;
                            }

                            long higherCommitIndex = -1;
                            ReplicationDocumentWithClusterInformation newestTopology = null;
                            OperationMetadata newestOperationMetadata = null;

                            for (OperationMetadata operationMetadata : nodes) {
                                try {
                                    ReplicationDocumentWithClusterInformation replicationDocumentWithClusterInformation = getReplicationDestionations.apply(operationMetadata);
                                    failureCounters.resetFailureCount(operationMetadata.getUrl());

                                    if (higherCommitIndex < replicationDocumentWithClusterInformation.getClusterCommitIndex()) {
                                        higherCommitIndex = replicationDocumentWithClusterInformation.getClusterCommitIndex();
                                        newestTopology = replicationDocumentWithClusterInformation;
                                        newestOperationMetadata = operationMetadata;
                                    }
                                } catch (Exception e) {
                                    //ignore
                                }
                            }

                            if (newestTopology == null && failoverServers != null && failoverServers.length > 0 && !tryFailoverServer) {
                                tryFailoverServer = true;
                            }

                            if (newestTopology == null && triedFailoverServer) {
                                setLeaderNode(primaryNode);
                                ClusterAwareRequestExecuter.this.nodes = Arrays.asList(primaryNode);
                                return;
                            }

                            if (newestTopology != null) {
                                ClusterAwareRequestExecuter.this.nodes = getNodes(newestOperationMetadata, newestTopology);
                                setLeaderNode(getLeaderNode(ClusterAwareRequestExecuter.this.nodes));

                                ReplicationInformerLocalCache.trySavingClusterNodesToLocalCache(serverHash, ClusterAwareRequestExecuter.this.nodes);

                                if (getLeaderNode() != null) {
                                    return;
                                }
                            }

                            TimeUtils.cleanSleep(500);
                        } finally {
                            lastUpdate = new Date();
                            ClusterAwareRequestExecuter.this.refreshReplicationInformationTask = null;
                        }
                    }
                }
            };

            this.refreshReplicationInformationTask = new Thread(refreshTask, "Update replication info for Cluster");
            this.refreshReplicationInformationTask.start();
        }
    }

    private static OperationMetadata getLeaderNode(Collection<OperationMetadata> nodes) {
        for (OperationMetadata node : nodes) {
            if (node.getClusterInformation() != null && node.getClusterInformation().isLeader()) {
                return node;
            }
        }
        return null;
    }

    private static List<OperationMetadata> getNodes(OperationMetadata node, ReplicationDocumentWithClusterInformation replicationDocument) {
        List<OperationMetadata> result = new ArrayList<>();
        for (ReplicationDestinationWithClusterInformation x : replicationDocument.getDestinations()) {
            OperationMetadata meta = convertReplicationDestinationToOperationMetadata(x, x.getClusterInformation());
            if (meta != null) {
                result.add(meta);
            }
        }

        result.add(new OperationMetadata(node.getUrl(), node.getCredentials(), replicationDocument.getClusterInformation()));
        return result;
    }

    private static OperationMetadata convertReplicationDestinationToOperationMetadata(ReplicationDestination destination, ClusterInformation clusterInformation) {
        String url = StringUtils.isEmpty(destination.getClientVisibleUrl()) ? destination.getUrl() : destination.getClientVisibleUrl();
        if (StringUtils.isEmpty(url) || destination.getDisabled() || destination.getIgnoredClient()) {
            return null;
        }

        if (StringUtils.isEmpty(destination.getDatabase())) {
            return new OperationMetadata(url, new OperationCredentials(destination.getApiKey()), clusterInformation);
        }

        return new OperationMetadata(RavenUrlExtensions.forDatabase(MultiDatabase.getRootDatabaseUrl(url), destination.getDatabase()), new OperationCredentials(destination.getApiKey()), clusterInformation);
    }

    @Override
    public CleanCloseable forceReadFromMaster() {
        return new CleanCloseable() {
            @Override
            public void close() {
                // empty
            }
        };
    }

    @Override
    public void addFailoverStatusChanged(EventHandler<ReplicationInformer.FailoverStatusChangedEventArgs> event) {
        // empty by design
    }

    @Override
    public void removeFailoverStatusChanged(EventHandler<ReplicationInformer.FailoverStatusChangedEventArgs> event) {
        // empty by design
    }
}
