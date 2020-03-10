package net.ravendb.client.test.cluster;

import com.google.common.base.Stopwatch;
import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.changes.DocumentChange;
import net.ravendb.client.documents.changes.IChangesObservable;
import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.changes.Observers;
import net.ravendb.client.documents.operations.identities.NextIdentityForOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.AllTopologyNodesDownException;
import net.ravendb.client.http.CurrentIndexAndNode;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.Topology;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.Tuple;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.DatabaseTopology;
import net.ravendb.client.serverwide.commands.GetDatabaseTopologyCommand;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.operations.ReorderDatabaseMembersOperation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class ClusterOperationTest extends ClusterTestBase {

    @Test
    public void nextIdentityForOperationShouldBroadcast() throws Exception {
        try (ClusterController cluster = createRaftCluster(3)) {

            String database = getDatabaseName();
            int numberOfNodes = 3;

            cluster.createDatabase(new DatabaseRecord(database), numberOfNodes, cluster.getInitialLeader().getUrl());

            try (IDocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), database)) {
                store.initialize();

                RequestExecutor re = store.getRequestExecutor(database);
                Long result = store.maintenance().forDatabase(database).send(new NextIdentityForOperation("person|"));
                assertThat(result)
                        .isEqualTo(1L);

                CurrentIndexAndNode preferred = re.getPreferredNode();
                String tag = preferred.currentNode.getClusterTag();

                cluster.executeJsScript(tag,
                        "server.ServerStore.InitializationCompleted.Reset(true);" +
                                " server.ServerStore.Initialized = false; " +
                                " var leader = server.ServerStore.Engine.CurrentLeader; " +
                                " if (leader) leader.StepDown();");

                Stopwatch sw = Stopwatch.createStarted();
                result = store.maintenance().forDatabase(database).send(new NextIdentityForOperation("person|"));
                sw.stop();
                assertThat(sw.elapsed())
                        .isLessThan(Duration.ofSeconds(10));

                CurrentIndexAndNode newPreferred = re.getPreferredNode();
                assertThat(newPreferred.currentNode.getClusterTag())
                        .isNotEqualTo(tag);
                assertThat(result)
                        .isEqualTo(2);
            }
        }
    }

    @Test
    public void nextIdentityForOperationShouldBroadcastAndFail() throws Exception {
        try (ClusterController cluster = createRaftCluster(3)) {

            String database = getDatabaseName();
            int numberOfNodes = 3;

            cluster.createDatabase(new DatabaseRecord(database), numberOfNodes, cluster.getInitialLeader().getUrl());

            try (IDocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), database)) {
                store.initialize();

                Long result = store.maintenance().forDatabase(database).send(new NextIdentityForOperation("person|"));
                assertThat(result)
                        .isEqualTo(1L);

                ClusterNode node = cluster
                        .nodes
                        .stream()
                        .filter(x -> !x.isLeader())
                        .findFirst()
                        .orElse(null);

                String leaderNodeTag = cluster.getCurrentLeader(store);

                cluster.executeJsScript(node.getNodeTag(),
                        "server.ServerStore.InitializationCompleted.Reset(true);\n" +
                                " server.ServerStore.Initialized = false;");

                cluster.disposeServer(leaderNodeTag);

                Stopwatch sw = Stopwatch.createStarted();
                assertThatThrownBy(() -> store.maintenance().forDatabase(database).send(new NextIdentityForOperation("person|")))
                        .isInstanceOf(AllTopologyNodesDownException.class)
                        .hasMessageContaining("Request to a server has failed.")
                        .hasMessageContaining("there is no leader, and we timed out waiting for one")
                        .hasMessageContaining("failed with timeout after 00:00:30");

                assertThat(sw.elapsed())
                        .isLessThan(Duration.ofSeconds(45));
            }
        }
    }

    @Test
    public void changesApiFailOver() throws Exception {
        String db = "Test";

        DatabaseTopology topology = new DatabaseTopology();
        topology.setDynamicNodesDistribution(true);

        Map<String, String> customSettings = new HashMap<>();
        customSettings.put("Cluster.TimeBeforeAddingReplicaInSec", "1");
        customSettings.put("Cluster.TimeBeforeMovingToRehabInSec", "0");
        customSettings.put("Cluster.StatsStabilizationTimeInSec", "1");
        customSettings.put("Cluster.ElectionTimeoutInMs", "50");

        try (ClusterController cluster = createRaftCluster(3, customSettings)) {
            DatabaseRecord databaseRecord = new DatabaseRecord();
            databaseRecord.setDatabaseName(db);
            databaseRecord.setTopology(topology);
            cluster.createDatabase(databaseRecord, 2, cluster.getInitialLeader().getUrl());

            try (IDocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), db)) {
                store.initialize();

                BlockingQueue<DocumentChange> list = new ArrayBlockingQueue<>(20);
                IDatabaseChanges taskObservable = store.changes();
                taskObservable.ensureConnectedNow();

                IChangesObservable<DocumentChange> observableWithTask = taskObservable.forDocument("users/1");
                observableWithTask.subscribe(Observers.create(list::add));

                try (IDocumentSession session = store.openSession()) {
                    session.store(new User(), "users/1");
                    session.saveChanges();
                }

                waitForDocument(User.class, store, "users/1");

                int value = waitForValue(() -> list.size(), 1);
                assertThat(value)
                        .isEqualTo(1);

                String currentUrl = store.getRequestExecutor().getUrl();
                cluster.disposeServer(cluster.getNodeByUrl(currentUrl).getNodeTag());

                taskObservable.ensureConnectedNow();

                waitForTopologyStabilization(db, cluster.getWorkingServer().getUrl(), 1, 2);

                try (IDocumentSession session = store.openSession()) {
                    session.store(new User(), "users/1");
                    session.saveChanges();
                }

                value = waitForValue(() -> list.size(), 2);
                assertThat(value)
                        .isEqualTo(2);

                currentUrl = store.getRequestExecutor().getUrl();

                cluster.disposeServer(cluster.getNodeByUrl(currentUrl).getNodeTag());

                taskObservable.ensureConnectedNow();

                waitForTopologyStabilization(db, cluster.getWorkingServer().getUrl(), 2, 1);

                try (IDocumentSession session = store.openSession()) {
                    session.store(new User(), "users/1");
                    session.saveChanges();
                }

                value = waitForValue(() -> list.size(), 3);
                assertThat(value)
                        .isEqualTo(3);
            }
        }
    }

    @Test
    public void changesApiReorderDatabaseNodes() throws Exception {
        String db = "ReorderDatabaseNodes";

        try (ClusterController cluster = createRaftCluster(2)) {
            cluster.createDatabase(db, 2, cluster.getInitialLeader().getUrl());


            ClusterNode leader = cluster.getInitialLeader();
            try (IDocumentStore store = new DocumentStore(leader.getUrl(), db)) {
                store.initialize();

                BlockingQueue<DocumentChange> list = new ArrayBlockingQueue<>(20);
                IDatabaseChanges taskObservable = store.changes();
                taskObservable.ensureConnectedNow();

                IChangesObservable<DocumentChange> observable = taskObservable.forDocument("users/1");
                observable.subscribe(Observers.create(list::add));


                try (IDocumentSession session = store.openSession()) {
                    session.store(new User(), "users/1");
                    session.saveChanges();
                }

                String url1 = store.getRequestExecutor().getUrl();
                assertThat(waitForDocument(User.class, store, "users/1"))
                        .isTrue();
                int value = waitForValue(() -> list.size(), 1);
                assertThat(value)
                        .isOne();

                reverseOrderSuccessfully(store, db);

                waitForValue(() -> {
                    String url2 = store.getRequestExecutor().getUrl();
                    return !url1.equals(url2);
                }, true);

                try (IDocumentSession session = store.openSession()) {
                    session.store(new User(), "users/1");
                    session.saveChanges();
                }

                value = waitForValue(() -> list.size(), 2);
                assertThat(value)
                        .isEqualTo(2);
            }
        }
    }

    public void waitForTopologyStabilization(String s, String serverUrl, int rehabCount, int memberCount) throws InterruptedException {
        try (IDocumentStore tempStore = new DocumentStore(serverUrl, s)) {
            tempStore.getConventions().setDisableTopologyUpdates(true);
            tempStore.initialize();

            Tuple<Integer, Integer> value = waitForValue(() -> {
                GetDatabaseTopologyCommand topologyGetCommand = new GetDatabaseTopologyCommand();
                tempStore.getRequestExecutor().execute(topologyGetCommand);
                Topology topo = topologyGetCommand.getResult();

                int rehab = 0;
                int members = 0;

                for (ServerNode n : topo.getNodes()) {
                    switch (n.getServerRole()) {
                        case REHAB:
                            rehab++;
                            break;
                        case MEMBER:
                            members++;
                            break;
                    }
                }

                return Tuple.create(rehab, members);
            }, Tuple.create(rehabCount, memberCount));
        }
    }

    public static void reverseOrderSuccessfully(IDocumentStore store, String db) {
        DatabaseRecordWithEtag record = store.maintenance().server().send(new GetDatabaseRecordOperation(db));
        Collections.reverse(record.getTopology().getMembers());

        List<String> copy = new ArrayList<>(record.getTopology().getMembers());
        store.maintenance().server().send(new ReorderDatabaseMembersOperation(db, record.getTopology().getMembers()));
        record = store.maintenance().server().send(new GetDatabaseRecordOperation(db));
    }
}
