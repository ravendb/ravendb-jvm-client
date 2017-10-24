package net.ravendb.client.http;

import net.ravendb.client.documents.conventions.DocumentConventions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class ClusterRequestExecutor extends RequestExecutor {

    private final Semaphore clusterTopologySemaphone = new Semaphore(1);

    protected ClusterRequestExecutor(DocumentConventions conventions) {
        super(null, conventions);

        /* TODO
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
        url = validateUrls(new String[] { url })[0];

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

    /* TODO:


        protected override Task PerformHealthCheck(ServerNode serverNode, int nodeIndex, JsonOperationContext context)
        {
            return ExecuteAsync(serverNode, nodeIndex, context, new GetTcpInfoCommand("health-check"), shouldRetry: false);
        }

        public override async Task<bool> UpdateTopologyAsync(ServerNode node, int timeout, bool forceUpdate = false)
        {
            if (_disposed)
                return false;
            var lockTaken = await _clusterTopologySemaphore.WaitAsync(timeout).ConfigureAwait(false);
            if (lockTaken == false)
                return false;
            try
            {
                if (_disposed)
                    return false;

                using (ContextPool.AllocateOperationContext(out JsonOperationContext context))
                {
                    var command = new GetClusterTopologyCommand();
                    await ExecuteAsync(node, null, context, command, shouldRetry: false).ConfigureAwait(false);

                    var serverHash = ServerHash.GetServerHash(node.Url);
                    ClusterTopologyLocalCache.TrySavingTopologyToLocalCache(serverHash, command.Result, context);

                    var results = command.Result;
                    var newTopology = new Topology
                    {
                        Nodes = new List<ServerNode>(
                            from member in results.Topology.Members
                            select new ServerNode
                            {
                                Url = member.Value,
                                ClusterTag = member.Key
                            }
                        )
                    };

                    if (_nodeSelector == null)
                    {
                        _nodeSelector = new NodeSelector(newTopology);
                        if (_readBalanceBehavior == ReadBalanceBehavior.FastestNode)
                        {
                            _nodeSelector.ScheduleSpeedTest();
                        }
                    }
                    else if (_nodeSelector.OnUpdateTopology(newTopology, forceUpdate: forceUpdate))
                    {
                        DisposeAllFailedNodesTimers();

                        if (_readBalanceBehavior == ReadBalanceBehavior.FastestNode)
                        {
                            _nodeSelector.ScheduleSpeedTest();
                        }
                    }

                    OnTopologyUpdated(newTopology);
                }
            }
            finally
            {
                _clusterTopologySemaphore.Release();
            }
            return true;
        }

        protected override Task UpdateClientConfigurationAsync()
        {
            return Task.CompletedTask;
        }

        public override void Dispose()
        {
            _clusterTopologySemaphore.Wait();
            base.Dispose();
        }

    }
     */
    //TODO:
}
