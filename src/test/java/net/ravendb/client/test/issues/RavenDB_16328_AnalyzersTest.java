package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.FieldIndexing;
import net.ravendb.client.documents.indexes.IndexErrors;
import net.ravendb.client.documents.indexes.analysis.AnalyzerDefinition;
import net.ravendb.client.documents.operations.indexes.ResetIndexOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.serverwide.operations.analyzers.DeleteServerWideAnalyzerOperation;
import net.ravendb.client.serverwide.operations.analyzers.PutServerWideAnalyzersOperation;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_16328_AnalyzersTest extends RemoteTestBase {

    public static String analyzer = "using System.IO;\n" +
            "using Lucene.Net.Analysis;\n" +
            "using Lucene.Net.Analysis.Standard;\n" +
            "\n" +
            "namespace SlowTests.Data.RavenDB_14939\n" +
            "{\n" +
            "    public class MyAnalyzer : StandardAnalyzer\n" +
            "    {\n" +
            "        public MyAnalyzer()\n" +
            "            : base(Lucene.Net.Util.Version.LUCENE_30)\n" +
            "        {\n" +
            "        }\n" +
            "\n" +
            "        public override TokenStream TokenStream(string fieldName, TextReader reader)\n" +
            "        {\n" +
            "            return new ASCIIFoldingFilter(base.TokenStream(fieldName, reader));\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    @Test
    public void canUseCustomAnalyzer() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String analyzerName = "MyAnalyzer";

            assertThatThrownBy(() -> {
                store.executeIndex(new MyIndex(analyzerName));
            }).hasMessageContaining("Cannot find analyzer type '" + analyzerName + "' for field: name");

            try {
                AnalyzerDefinition analyzerDefinition = new AnalyzerDefinition();
                analyzerDefinition.setName(analyzerName);
                analyzerDefinition.setCode(analyzer);

                store.maintenance().server().send(new PutServerWideAnalyzersOperation(analyzerDefinition));

                store.executeIndex(new MyIndex(analyzerName));

                fill(store);

                waitForIndexing(store);

                assertCount(store, MyIndex.class);

                store.maintenance().server().send(new DeleteServerWideAnalyzerOperation(analyzerName));

                store.maintenance().send(new ResetIndexOperation(new MyIndex(analyzerName).getIndexName()));

                IndexErrors[] errors = waitForIndexingErrors(store, Duration.ofSeconds(10));
                assertThat(errors)
                        .hasSize(1);
                assertThat(errors[0].getErrors())
                        .hasSize(1);
                assertThat(errors[0].getErrors()[0].getError())
                        .contains("Cannot find analyzer type '" + analyzerName + "' for field: name");
            } finally {
                store.maintenance().server().send(new DeleteServerWideAnalyzerOperation(analyzerName));
            }
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

    private static <T extends AbstractIndexCreationTask> void assertCount(IDocumentStore store, Class<T> index) {
        assertCount(store, index, 4);
    }

    private static <T extends AbstractIndexCreationTask> void assertCount(IDocumentStore store, Class<T> index, int expectedCount) {
        waitForIndexing(store);

        try (IDocumentSession session = store.openSession()) {
            List<Customer> results = session.query(Customer.class, index)
                    .noCaching()
                    .search("name", "Rogério*")
                    .toList();

            assertThat(results)
                    .hasSize(expectedCount);
        }
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

    public static class MyIndex_WithoutAnalyzer extends AbstractIndexCreationTask {
        public MyIndex_WithoutAnalyzer() {
            map = "from customer in docs.Customers select new { customer.name }";
            index("name", FieldIndexing.SEARCH);
        }
    }
}

