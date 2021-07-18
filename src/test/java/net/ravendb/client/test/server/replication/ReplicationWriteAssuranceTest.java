package net.ravendb.client.test.server.replication;

import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class ReplicationWriteAssuranceTest extends ClusterTestBase {

    @Test
    public void serverSideWriteAssurance() throws Exception {
        try (ClusterController cluster = createRaftCluster(3)) {

            ClusterNode initialLeader = cluster.getInitialLeader();

            String database = getDatabaseName();
            int numberOfNodes = 3;

            cluster.createDatabase(new DatabaseRecord(database), numberOfNodes, cluster.getInitialLeader().getUrl());

            try (IDocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), database)) {
                store.initialize();

                try (IDocumentSession session = store.openSession()) {
                    session.advanced().waitForReplicationAfterSaveChanges(x -> x.numberOfReplicas(2).withTimeout(Duration.ofSeconds(30)));

                    User user = new User();
                    user.setName("Idan");
                    session.store(user, "users/1");
                    session.saveChanges();
                }
            }

            for (ClusterNode node : cluster.nodes) {
                try (IDocumentStore store = new DocumentStore(node.getUrl(), database)) {
                    store.getConventions().setDisableTopologyUpdates(true);
                    store.initialize();

                    try (IDocumentSession session = store.openSession()) {
                        assertThat(session.load(User.class, "users/1"))
                        .isNotNull();
                    }
                }
            }
        }
    }
}
