package net.ravendb.client.test.issues;

import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.Lazy;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class RavenDB_18364Test extends ClusterTestBase {

    @Test
    public void lazilyLoad_WhenCachedResultAndFailover_ShouldNotReturnReturnNull() throws Exception {

        try (ClusterController cluster = createRaftCluster(2)) {
            String databaseName = getDatabaseName();

            cluster.createDatabase(new DatabaseRecord(databaseName), 2, cluster.getInitialLeader().getUrl());

            try (IDocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), databaseName)) {
                store.initialize();

                String id = "testObjs/0";

                try (IDocumentSession session = store.openSession()) {
                    TestObj o = new TestObj();
                    o.setLargeContent("abcd");
                    session.store(o, id);
                    session.advanced().waitForReplicationAfterSaveChanges(x -> x.numberOfReplicas(1));
                    session.saveChanges();
                }

                AtomicReference<String> firstNodeUrl = new AtomicReference<>();

                try (IDocumentSession session = store.openSession()) {
                    session.advanced().getRequestExecutor().addOnSucceedRequestListener((sender, event) -> {
                        try {
                            URI uri = new URI(event.getUrl());

                            firstNodeUrl.set("http://" + uri.getHost() + ":" + uri.getPort());
                        } catch (URISyntaxException e) {
                            throw new RuntimeException(e);
                        }

                    });
                    session.load(TestObj.class, id);
                }

                ClusterNode firstServer = cluster.nodes.stream().filter(x -> x.getUrl().equals(firstNodeUrl.get())).findFirst().get();

                cluster.disposeServer(firstServer.getNodeTag());

                try (IDocumentSession session = store.openSession()) {
                    Lazy<TestObj> lazilyLoaded0 = session.advanced().lazily().load(TestObj.class, id);
                    TestObj loaded0 = lazilyLoaded0.getValue();
                    assertThat(loaded0)
                            .isNotNull();
                }

                try (IDocumentSession session = store.openSession()) {
                    Lazy<TestObj> lazilyLoaded0 = session.advanced().lazily().load(TestObj.class, id);
                    TestObj loaded0 = lazilyLoaded0.getValue();
                    assertThat(loaded0)
                            .isNotNull();
                }
            }
        }
    }

    public static class TestObj {
        private String id;
        private String largeContent;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLargeContent() {
            return largeContent;
        }

        public void setLargeContent(String largeContent) {
            this.largeContent = largeContent;
        }
    }
}
