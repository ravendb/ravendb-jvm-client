package net.ravendb.client.test.issues;

import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionInfo;
import net.ravendb.client.http.LoadBalanceBehavior;
import net.ravendb.client.http.ReadBalanceBehavior;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14978Test extends ClusterTestBase {

    @Test
    public void can_setup_write_load_balancing() throws Exception {
        int numberOfNodes = 3;
        try (ClusterController cluster = createRaftCluster(numberOfNodes)) {
            String databaseName = getDatabaseName();

            String[] context = new String[] { "users/1" };

            cluster.createDatabase(new DatabaseRecord(databaseName), numberOfNodes, cluster.getInitialLeader().getUrl());

            try (IDocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), databaseName)) {
                store.getConventions().setReadBalanceBehavior(ReadBalanceBehavior.ROUND_ROBIN);
                store.getConventions().setLoadBalanceBehavior(LoadBalanceBehavior.USE_SESSION_CONTEXT);
                store.getConventions().setLoadBalancerPerSessionContextSelector(x -> context[0]);
                store.initialize();

                try (IDocumentSession s0 = store.openSession()) {
                    s0.load(User.class, "test/1");
                }

                int s1Ctx = -1;

                try (IDocumentSession s1 = store.openSession()) {
                    SessionInfo sessionInfo = s1.advanced().getSessionInfo();
                    s1Ctx = sessionInfo.getSessionId();
                }

                int s2Ctx = -1;

                try (IDocumentSession s2 = store.openSession()) {
                    SessionInfo sessionInfo = s2.advanced().getSessionInfo();
                    s2Ctx = sessionInfo.getSessionId();
                }

                assertThat(s2Ctx)
                        .isEqualTo(s1Ctx);

                context[0] = "users/2";

                int s3Ctx = -1;
                try (IDocumentSession s3 = store.openSession()) {
                    SessionInfo sessionInfo = s3.advanced().getSessionInfo();
                    s3Ctx = sessionInfo.getSessionId();
                }

                assertThat(s3Ctx)
                        .isNotEqualTo(s2Ctx);

                int s4Ctx = -1;
                try (IDocumentSession s4 = store.openSession()) {
                    s4.advanced().getSessionInfo().setContext("monkey");

                    SessionInfo sessionInfo = s4.advanced().getSessionInfo();
                    s3Ctx = sessionInfo.getSessionId();
                }

                assertThat(s4Ctx)
                        .isNotEqualTo(s3Ctx);
            }
        }
    }
}
