package net.ravendb.client.test.cluster;

import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionInfo;
import net.ravendb.client.http.*;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.commands.GetDatabaseTopologyCommand;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterModesForRequestExecutorTest extends ClusterTestBase {

    @Test
    public void round_robin_load_balancing_should_work() throws Exception {
        try (ClusterController cluster = createRaftCluster(3)) {

            ClusterNode initialLeader = cluster.getInitialLeader();
            List<ClusterNode> followers = cluster
                    .nodes
                    .stream()
                    .filter(x -> !x.isLeader())
                    .collect(Collectors.toList());

            DocumentConventions conventionsForLoadBalancing = new DocumentConventions();
            conventionsForLoadBalancing.setReadBalanceBehavior(ReadBalanceBehavior.ROUND_ROBIN);

            String databaseName = getDatabaseName();
            int numberOfNodes = 3;

            cluster.createDatabase(new DatabaseRecord(databaseName), numberOfNodes, cluster.getInitialLeader().getUrl());

            try (DocumentStore leaderStore = new DocumentStore(cluster.getInitialLeader().getUrl(), databaseName)) {
                leaderStore.setConventions(conventionsForLoadBalancing);

                try (DocumentStore follower1 = new DocumentStore(followers.get(0).getUrl(), databaseName)) {
                    follower1.setConventions(conventionsForLoadBalancing);

                    try (DocumentStore follower2 = new DocumentStore(followers.get(1).getUrl(), databaseName)) {
                        follower2.setConventions(conventionsForLoadBalancing);

                        leaderStore.initialize();
                        follower1.initialize();
                        follower2.initialize();

                        RequestExecutor leaderRequestExecutor = leaderStore.getRequestExecutor();

                        //make sure we have updated topology --> more deterministic test
                        ServerNode serverNode = new ServerNode();
                        serverNode.setClusterTag("A");
                        serverNode.setDatabase(databaseName);
                        serverNode.setUrl(initialLeader.getUrl());

                        UpdateTopologyParameters updateTopologyParameters = new UpdateTopologyParameters(serverNode);
                        updateTopologyParameters.setTimeoutInMs(5000);
                        updateTopologyParameters.setForceUpdate(true);

                        leaderRequestExecutor.updateTopologyAsync(updateTopologyParameters).get();

                        //wait until all nodes in database cluster are members (and not promotables)
                        //GetDatabaseTopologyCommand -> does not retrieve promotables

                        Topology topology = new Topology();
                        while (topology.getNodes() == null || topology.getNodes().size() != 3) {
                            GetDatabaseTopologyCommand topologyGetCommand = new GetDatabaseTopologyCommand();
                            leaderRequestExecutor.execute(topologyGetCommand);

                            topology = topologyGetCommand.getResult();
                            Thread.sleep(50);
                        }

                        try (IDocumentSession session = leaderStore.openSession()) {
                            User user1 = new User();
                            user1.setName("John Dow");
                            session.store(user1);

                            User user2 = new User();
                            user2.setName("Jack Dow");
                            session.store(user2);

                            User user3 = new User();
                            user3.setName("Jane Dow");
                            session.store(user3);

                            User marker = new User();
                            marker.setName("FooBar");
                            session.store(marker, "marker");

                            session.saveChanges();

                            waitForDocumentInCluster(User.class, (DocumentSession) session,
                                    "marker", (doc) -> Boolean.TRUE, Duration.ofSeconds(10));
                        }

                        List<String> usedUrls = new ArrayList<>();

                        for (int i = 0; i < 3; i++) {
                            try (IDocumentSession session = leaderStore.openSession()) {
                                session.query(User.class)
                                        .whereStartsWith("name", "Ja")
                                        .toList();

                                usedUrls.add(session.advanced().getCurrentSessionNode().getUrl().toLowerCase());
                            }
                        }

                        assertThat(new HashSet<>(usedUrls))
                                .hasSize(3);
                    }
                }
            }
        }
    }

    @Test
    public void round_robin_load_balancing_with_failing_node_should_work() throws Exception {
        try (ClusterController cluster = createRaftCluster(3)) {

            ClusterNode initialLeader = cluster.getInitialLeader();
            List<ClusterNode> followers = cluster
                    .nodes
                    .stream()
                    .filter(x -> !x.isLeader())
                    .collect(Collectors.toList());

            DocumentConventions conventionsForLoadBalancing = new DocumentConventions();
            conventionsForLoadBalancing.setReadBalanceBehavior(ReadBalanceBehavior.ROUND_ROBIN);

            String databaseName = getDatabaseName();
            int numberOfNodes = 3;

            cluster.createDatabase(new DatabaseRecord(databaseName), numberOfNodes, cluster.getInitialLeader().getUrl());

            try (DocumentStore leaderStore = new DocumentStore(cluster.getInitialLeader().getUrl(), databaseName)) {
                leaderStore.setConventions(conventionsForLoadBalancing);

                try (DocumentStore follower1 = new DocumentStore(followers.get(0).getUrl(), databaseName)) {
                    follower1.setConventions(conventionsForLoadBalancing);

                    try (DocumentStore follower2 = new DocumentStore(followers.get(1).getUrl(), databaseName)) {
                        follower2.setConventions(conventionsForLoadBalancing);

                        leaderStore.initialize();
                        follower1.initialize();
                        follower2.initialize();

                        RequestExecutor leaderRequestExecutor = leaderStore.getRequestExecutor();

                        //make sure we have updated topology --> more deterministic test
                        ServerNode serverNode = new ServerNode();
                        serverNode.setClusterTag("A");
                        serverNode.setDatabase(databaseName);
                        serverNode.setUrl(initialLeader.getUrl());

                        UpdateTopologyParameters updateTopologyParameters = new UpdateTopologyParameters(serverNode);
                        updateTopologyParameters.setTimeoutInMs(5000);
                        updateTopologyParameters.setForceUpdate(true);

                        leaderRequestExecutor.updateTopologyAsync(updateTopologyParameters).get();

                        //wait until all nodes in database cluster are members (and not promotables)
                        //GetDatabaseTopologyCommand -> does not retrieve promotables

                        Topology topology = new Topology();
                        while (topology.getNodes() == null || topology.getNodes().size() != 3) {
                            GetDatabaseTopologyCommand topologyGetCommand = new GetDatabaseTopologyCommand();
                            leaderRequestExecutor.execute(topologyGetCommand);

                            topology = topologyGetCommand.getResult();
                            Thread.sleep(50);
                        }

                        try (IDocumentSession session = leaderStore.openSession()) {
                            User user1 = new User();
                            user1.setName("John Dow");
                            session.store(user1);

                            User user2 = new User();
                            user2.setName("Jack Dow");
                            session.store(user2);

                            User user3 = new User();
                            user3.setName("Jane Dow");
                            session.store(user3);

                            User marker = new User();
                            marker.setName("FooBar");
                            session.store(marker, "marker");

                            session.saveChanges();

                            waitForDocumentInCluster(User.class, (DocumentSession) session,
                                    "marker", (doc) -> Boolean.TRUE, Duration.ofSeconds(10));
                        }

                        try (RequestExecutor requestExecutor = RequestExecutor.create(follower1.getUrls(), databaseName,
                                null, null, null, follower1.getExecutorService(), follower1.getConventions())) {
                            do {
                                Thread.sleep(100);
                            } while (requestExecutor.getTopologyNodes() == null);

                            cluster.disposeServer(initialLeader.getNodeTag());

                            Set<String> failedRequests = new HashSet<>();

                            requestExecutor.addOnFailedRequestListener((sender, event) -> {
                                failedRequests.add(event.getUrl());
                            });

                            for (int sessionId = 0; sessionId < 5; sessionId++) {
                                requestExecutor.getCache().clear(); // make sure we do not use request cache
                                RavenCommand<DatabaseStatistics> command = new GetStatisticsOperation().getCommand(new DocumentConventions());
                                requestExecutor.execute(command);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void RavenDB_7992() throws Exception {
        //here we test that when choosing Fastest-Node as the ReadBalanceBehavior,
        //we can execute commands that use a context, without it leading to a race condition
        try (ClusterController cluster = createRaftCluster(3)) {

            ClusterNode initialLeader = cluster.getInitialLeader();

            DocumentConventions conventionsForLoadBalancing = new DocumentConventions();
            conventionsForLoadBalancing.setReadBalanceBehavior(ReadBalanceBehavior.FASTEST_NODE);

            String databaseName = getDatabaseName();
            int numberOfNodes = 3;

            cluster.createDatabase(new DatabaseRecord(databaseName), numberOfNodes, cluster.getInitialLeader().getUrl());

            try (DocumentStore leaderStore = new DocumentStore(initialLeader.getUrl(), databaseName)) {
                leaderStore.setConventions(conventionsForLoadBalancing);
                leaderStore.initialize();

                try (IDocumentSession session = leaderStore.openSession()) {
                    User user = new User();
                    user.setName("Jon Snow");
                    session.store(user);
                    session.saveChanges();
                }

                try (IDocumentSession session = leaderStore.openSession()) {
                    session.query(User.class)
                            .whereStartsWith("name", "Jo");
                }
            }
        }
    }
}
