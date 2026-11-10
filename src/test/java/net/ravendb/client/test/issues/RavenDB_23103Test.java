package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.operations.DeleteByQueryOperation;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.operations.PatchByQueryOperation;
import net.ravendb.client.documents.queries.IndexPatchOptions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.documents.queries.QueryOperationOptions;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "6.2")
public class RavenDB_23103Test extends RemoteTestBase {

    private static final int NUMBER_OF_COMPANIES = 1024;

    private IDocumentStore getStoreWithCompanies(boolean createIndex) throws Exception {
        IDocumentStore store = getDocumentStore("test_db", false, null,
                record -> record.getSettings().put("Indexing.MapBatchSize", "128"));

        if (createIndex) {
            new Companies_ByName().execute(store);
        }

        try (BulkInsertOperation bulk = store.bulkInsert()) {
            for (int i = 0; i < NUMBER_OF_COMPANIES; i++) {
                Company company = new Company();
                company.setName("Name1");
                bulk.store(company, "Companies/" + i);
            }
        }

        waitForIndexing(store);
        return store;
    }

    private static int countCompaniesByName(IDocumentStore store, String name) {
        try (IDocumentSession session = store.openSession()) {
            return session.query(Company.class, Companies_ByName.class)
                    .whereEquals("name", name)
                    .count();
        }
    }

    private static QueryOperationOptions withIndexPatchOptions(IndexPatchOptions indexPatchOptions) {
        QueryOperationOptions options = new QueryOperationOptions();
        options.setIndexPatchOptions(indexPatchOptions);
        return options;
    }

    @Test
    public void waitForIndexesAfterPatch_Simple_Static() throws Exception {
        try (IDocumentStore store = getStoreWithCompanies(true)) {
            IndexQuery iq = new IndexQuery("from index 'Companies/ByName' as c update { c.name = 'Name2' }");

            IndexPatchOptions indexPatchOptions = new IndexPatchOptions(Duration.ofSeconds(30));
            indexPatchOptions.setThrowOnTimeoutInWaitForIndexes(true);

            Operation operation = store.operations().sendAsync(new PatchByQueryOperation(iq, withIndexPatchOptions(indexPatchOptions)));
            operation.waitForCompletion();

            // no waitForIndexing here - the patch operation waited for the index
            assertThat(countCompaniesByName(store, "Name2")).isEqualTo(NUMBER_OF_COMPANIES);
        }
    }

    @Test
    public void waitForIndexesAfterPatch_StaticIndex_WithTimeout_NoSpecificIndexes() throws Exception {
        try (IDocumentStore store = getStoreWithCompanies(true)) {
            IndexQuery iq = new IndexQuery("from index 'Companies/ByName' as c update { c.name = 'Name2' }");

            IndexPatchOptions indexPatchOptions = new IndexPatchOptions();
            indexPatchOptions.setWaitForIndexesTimeout(Duration.ofMillis(1));
            indexPatchOptions.setThrowOnTimeoutInWaitForIndexes(true);

            Operation operation = store.operations().sendAsync(new PatchByQueryOperation(iq, withIndexPatchOptions(indexPatchOptions)));

            assertThatThrownBy(operation::waitForCompletion).isInstanceOf(Exception.class);
        }
    }

    @Test
    public void waitForIndexesAfterPatch_StaticIndex_WithTimeout_WithSpecificIndexes() throws Exception {
        try (IDocumentStore store = getStoreWithCompanies(true)) {
            IndexQuery iq = new IndexQuery("from index 'Companies/ByName' as c update { c.name = 'Name3' }");

            IndexPatchOptions indexPatchOptions = new IndexPatchOptions();
            indexPatchOptions.setWaitForIndexesTimeout(Duration.ofMillis(1));
            indexPatchOptions.setWaitForSpecificIndexes(new String[]{"Companies/ByName"});
            indexPatchOptions.setThrowOnTimeoutInWaitForIndexes(true);

            Operation operation = store.operations().sendAsync(new PatchByQueryOperation(iq, withIndexPatchOptions(indexPatchOptions)));

            assertThatThrownBy(operation::waitForCompletion).isInstanceOf(Exception.class);
        }
    }

    @Test
    public void waitForIndexesAfterPatch_StaticIndex_WithoutTimeout_WithSpecificIndexes() throws Exception {
        try (IDocumentStore store = getStoreWithCompanies(true)) {
            IndexQuery iq = new IndexQuery("from index 'Companies/ByName' as c update { c.name = 'Name2' }");

            IndexPatchOptions indexPatchOptions = new IndexPatchOptions();
            indexPatchOptions.setWaitForSpecificIndexes(new String[]{"Companies/ByName"});
            indexPatchOptions.setThrowOnTimeoutInWaitForIndexes(true);

            Operation operation = store.operations().sendAsync(new PatchByQueryOperation(iq, withIndexPatchOptions(indexPatchOptions)));
            operation.waitForCompletion();

            assertThat(countCompaniesByName(store, "Name2")).isEqualTo(NUMBER_OF_COMPANIES);
        }
    }

    @Test
    public void waitForIndexesAfterPatch_Simple_CollectionQuery() throws Exception {
        try (IDocumentStore store = getStoreWithCompanies(true)) {
            IndexQuery iq = new IndexQuery("from companies as c update { c.name = 'Name2' }");

            IndexPatchOptions indexPatchOptions = new IndexPatchOptions();
            indexPatchOptions.setWaitForIndexesTimeout(Duration.ofSeconds(30));

            Operation operation = store.operations().sendAsync(new PatchByQueryOperation(iq, withIndexPatchOptions(indexPatchOptions)));
            operation.waitForCompletion();

            assertThat(countCompaniesByName(store, "Name2")).isEqualTo(NUMBER_OF_COMPANIES);
        }
    }

    @Test
    public void deleteByQuery_With_WaitForIndexes_Static() throws Exception {
        try (IDocumentStore store = getStoreWithCompanies(true)) {
            IndexQuery iq = new IndexQuery("from index 'Companies/ByName'");

            IndexPatchOptions indexPatchOptions = new IndexPatchOptions();
            indexPatchOptions.setWaitForIndexesTimeout(Duration.ofSeconds(30));

            QueryOperationOptions options = withIndexPatchOptions(indexPatchOptions);
            options.setAllowStale(false);

            Operation operation = store.operations().sendAsync(new DeleteByQueryOperation(iq, options));
            operation.waitForCompletion();

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.query(Company.class, Companies_ByName.class).count()).isZero();
            }
        }
    }

    @Test
    public void deleteByQuery_With_WaitForIndexes_Static_WithTimeout() throws Exception {
        try (IDocumentStore store = getStoreWithCompanies(true)) {
            IndexQuery iq = new IndexQuery("from index 'Companies/ByName'");

            IndexPatchOptions indexPatchOptions = new IndexPatchOptions();
            indexPatchOptions.setWaitForIndexesTimeout(Duration.ofMillis(1));
            indexPatchOptions.setThrowOnTimeoutInWaitForIndexes(true);

            Operation operation = store.operations().sendAsync(new DeleteByQueryOperation(iq, withIndexPatchOptions(indexPatchOptions)));

            assertThatThrownBy(operation::waitForCompletion).isInstanceOf(Exception.class);
        }
    }

    @Test
    public void indexPatchOptionsDefaults() {
        IndexPatchOptions options = new IndexPatchOptions();
        assertThat(options.getWaitForIndexesTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(options.isThrowOnTimeoutInWaitForIndexes()).isFalse();
        assertThat(options.getWaitForSpecificIndexes()).isNull();
    }

    public static class Companies_ByName extends AbstractIndexCreationTask {
        public Companies_ByName() {
            map = "from c in docs.Companies select new { c.name }";
        }
    }

    public static class Company {
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
}
