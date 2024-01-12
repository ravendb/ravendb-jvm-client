package net.ravendb.client.test.issues;

import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.subscriptions.SubscriptionWorker;
import net.ravendb.client.documents.subscriptions.SubscriptionWorkerOptions;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class RDBC_538Test extends ClusterTestBase {

    private final int _reasonableWaitTime = 60;

    @Test
    public void canHandleSubscriptionRedirect() throws Exception {
        try (ClusterController cluster = createRaftCluster(3)) {
            ClusterNode leader = cluster.getInitialLeader();

            String databaseName = getDatabaseName();

            // create database on single node

            DatabaseRecord databaseRecord = new DatabaseRecord(databaseName);

            cluster.createDatabase(databaseRecord, 3, cluster.getInitialLeader().getUrl());

            String id;

            // save document
            try (DocumentStore store = new DocumentStore(leader.getUrl(), databaseName)) {
                store.initialize();

                try (IDocumentSession session = store.openSession()) {
                    User user1 = new User();
                    user1.setAge(31);
                    session.store(user1, "users/1");

                    session.saveChanges();
                }

                id = store.subscriptions().create(User.class);
            }

            // now open store on leader
            try (DocumentStore store = new DocumentStore(leader.getUrl(), databaseName)) {
                store.initialize();

                try (SubscriptionWorker<User> subscription = store.subscriptions().getSubscriptionWorker(User.class, new SubscriptionWorkerOptions(id))) {

                    BlockingArrayQueue<String> keys = new BlockingArrayQueue<>();

                    subscription.run(batch -> batch.getItems().forEach(x -> keys.add(x.getId())));

                    String key = keys.poll(_reasonableWaitTime, TimeUnit.SECONDS);
                    assertThat(key)
                            .isNotNull()
                            .isEqualTo("users/1");

                    // drop subscription
                    store.subscriptions().delete(id);
                }
            }
        }
    }
}
