package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.indexes.IndexErrors;
import net.ravendb.client.documents.indexes.analysis.AnalyzerDefinition;
import net.ravendb.client.documents.operations.analyzers.DeleteAnalyzerOperation;
import net.ravendb.client.documents.operations.analyzers.PutAnalyzersOperation;
import net.ravendb.client.documents.operations.indexes.ResetIndexOperation;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.documents.compilation.IndexCompilationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

public class RavenDB_14939Test extends RemoteTestBase {

    @Test
    public void canUseCustomAnalyzerWithOperations() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String analyzerName = store.getDatabase();

            assertThatThrownBy(() -> store.executeIndex(new MyIndex(analyzerName)))
                    .isExactlyInstanceOf(IndexCompilationException.class)
                    .hasMessageContaining("Cannot find analyzer type '" + analyzerName + "' for field: name");


            AnalyzerDefinition analyzerDefinition = new AnalyzerDefinition();
            analyzerDefinition.setName(analyzerName);
            analyzerDefinition.setCode(getAnalyzer(analyzerName));
            store.maintenance().send(new PutAnalyzersOperation(analyzerDefinition));

            store.executeIndex(new MyIndex(analyzerName));

            fill(store);

            waitForIndexing(store);

            assertCount(MyIndex.class, store);

            store.maintenance().send(new DeleteAnalyzerOperation(analyzerName));

            store.maintenance().send(new ResetIndexOperation(new MyIndex(analyzerName).getIndexName()));

            IndexErrors[] errors = waitForIndexingErrors(store, Duration.ofSeconds(10));
            assertThat(errors)
                    .hasSize(1);
            assertThat(errors[0].getErrors())
                    .hasSize(1);
            assertThat(errors[0].getErrors()[0].getError())
                    .contains("Cannot find analyzer type '" + analyzerName + "' for field: name");
        }
    }

    private static void fill(IDocumentStore store) {
        try (IDocumentSession session = store.openSession()) {
            Customer c1 = new Customer();
            c1.setName("Rogério");
            session.store(c1);

            Customer c2 = new Customer();
            c2.setName("Rogerio");
            session.store(c2);

            Customer c3 = new Customer();
            c3.setName("Paulo Rogerio");
            session.store(c3);

            Customer c4 = new Customer();
            c4.setName("Paulo Rogério");
            session.store(c4);

            session.saveChanges();
        }
    }

    private static <T extends AbstractIndexCreationTask> void assertCount(Class<T> indexClass, IDocumentStore store) {
        waitForIndexing(store);

        try (IDocumentSession session = store.openSession()) {
            IDocumentQuery<Customer> results = session.query(Customer.class, indexClass)
                    .noCaching()
                    .search("name", "Rogério*");

            assertThat(results.count())
                    .isEqualTo(4);
        }
    }

    private static String getAnalyzer(String analyzerName) {
        return RavenDB_16328_AnalyzersTest.analyzer.replaceAll("MyAnalyzer", analyzerName);
    }

    public static class Customer {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class MyIndex extends AbstractIndexCreationTask {
        public MyIndex() {
            this("MyAnalyzer");
        }

        public MyIndex(String analyzerName) {
            map = "from customer in docs.Customers select new { customer.name }";

            index("name", FieldIndexing.SEARCH);
            analyze("name", analyzerName);
        }
    }
}
