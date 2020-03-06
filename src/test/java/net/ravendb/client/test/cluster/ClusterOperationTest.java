package net.ravendb.client.test.cluster;

import com.google.common.base.Stopwatch;
import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.identities.NextIdentityForOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.AllTopologyNodesDownException;
import net.ravendb.client.http.CurrentIndexAndNode;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Date;

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
}
