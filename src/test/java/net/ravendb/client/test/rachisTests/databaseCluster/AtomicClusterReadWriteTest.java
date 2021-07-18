package net.ravendb.client.test.rachisTests.databaseCluster;

import net.ravendb.client.ClusterTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.operations.compareExchange.GetCompareExchangeValuesOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.session.TransactionMode;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class AtomicClusterReadWriteTest extends ClusterTestBase {

    public static class TestObj {
        private String id;
        private String prop;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getProp() {
            return prop;
        }

        public void setProp(String prop) {
            this.prop = prop;
        }
    }

    @Test
    public void clusterWideTransaction_WhenStore_ShouldCreateCompareExchange() throws Exception {
        try (ClusterController cluster = createRaftCluster(3)) {
            String database = getDatabaseName();
            int numberOfNodes = 3;

            cluster.createDatabase(new DatabaseRecord(database), numberOfNodes, cluster.getInitialLeader().getUrl());

            try (IDocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), database)) {
                store.initialize();

                SessionOptions sessionOptions = new SessionOptions();
                sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);

                TestObj entity = new TestObj();

                try (IDocumentSession session = store.openSession(sessionOptions)) {
                    session.store(entity);
                    session.saveChanges();
                }

                Map<String, CompareExchangeValue<TestObj>> result = store.operations().send(
                        new GetCompareExchangeValuesOperation<>(TestObj.class, ""));
                assertThat(result)
                        .hasSize(1);
                assertThat(result.keySet().iterator().next())
                        .endsWith(entity.getId().toLowerCase());
            }
        }
    }

    @Test
    public void clusterWideTransaction_WhenDisableAndStore_ShouldNotCreateCompareExchange() throws Exception {
        try (ClusterController cluster = createRaftCluster(3)) {
            String database = getDatabaseName();
            int numberOfNodes = 3;

            cluster.createDatabase(new DatabaseRecord(database), numberOfNodes, cluster.getInitialLeader().getUrl());

            try (IDocumentStore store = new DocumentStore(cluster.getInitialLeader().getUrl(), database)) {
                store.initialize();

                SessionOptions sessionOptions = new SessionOptions();
                sessionOptions.setTransactionMode(TransactionMode.CLUSTER_WIDE);
                sessionOptions.setDisableAtomicDocumentWritesInClusterWideTransaction(true);

                TestObj entity = new TestObj();

                try (IDocumentSession session = store.openSession(sessionOptions)) {
                    session.store(entity);
                    session.saveChanges();
                }

                Map<String, CompareExchangeValue<TestObj>> result = store.operations().send(
                        new GetCompareExchangeValuesOperation<>(TestObj.class, ""));
                assertThat(result)
                        .hasSize(0);
            }
        }
    }
}
