package net.ravendb.client.test.issues;

import com.google.common.collect.Sets;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexConfiguration;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.indexes.GetIndexesOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.infrastructure.entities.Company;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14084Test extends RemoteTestBase {

    public static class Companies_ByUnknown extends AbstractIndexCreationTask {
        @Override
        public IndexDefinition createIndexDefinition() {
            IndexDefinition indexDefinition = new IndexDefinition();
            indexDefinition.setName("Companies/ByUnknown");
            indexDefinition.setMaps(Sets.newHashSet("from c in docs.Companies select new { Unknown = c.Unknown };"));
            return indexDefinition;
        }
    }

    public static class Companies_ByUnknown_WithIndexMissingFieldsAsNull extends AbstractIndexCreationTask {
        @Override
        public IndexDefinition createIndexDefinition() {
            IndexDefinition indexDefinition = new IndexDefinition();
            indexDefinition.setName("Companies/ByUnknown/WithIndexMissingFieldsAsNull");
            indexDefinition.setMaps(Sets.newHashSet("from c in docs.Companies select new { Unknown = c.Unknown };"));
            indexDefinition.getConfiguration().put("Indexing.IndexMissingFieldsAsNull", "true");
            return indexDefinition;
        }
    }

    @Test
    public void canIndexMissingFieldsAsNull_Static() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            new Companies_ByUnknown().execute(store);
            new Companies_ByUnknown_WithIndexMissingFieldsAsNull().execute(store);

            try (IDocumentSession session = store.openSession()) {
                Company company = new Company();
                company.setName("HR");
                session.store(company);

                session.saveChanges();
            }

            waitForIndexing(store);

            SessionOptions sessionOptions = new SessionOptions();
            sessionOptions.setNoCaching(true);

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                List<Company> companies = session
                        .advanced()
                        .documentQuery(Company.class, Companies_ByUnknown.class)
                        .whereEquals("Unknown", null)
                        .toList();

                assertThat(companies)
                        .hasSize(0);
            }

            try (IDocumentSession session = store.openSession(sessionOptions)) {
                List<Company> companies = session
                        .advanced()
                        .documentQuery(Company.class, Companies_ByUnknown_WithIndexMissingFieldsAsNull.class)
                        .whereEquals("Unknown", null)
                        .toList();

                assertThat(companies)
                        .hasSize(1);
            }

            IndexDefinition[] indexDefinitions = store.maintenance().send(new GetIndexesOperation(0, 10));
            assertThat(indexDefinitions)
                    .hasSize(2);

            IndexConfiguration configuration = Arrays.stream(indexDefinitions)
                    .filter(x -> x.getName().equals("Companies/ByUnknown/WithIndexMissingFieldsAsNull"))
                    .findFirst()
                    .get()
                    .getConfiguration();

            assertThat(configuration)
                    .hasSize(1)
                    .containsKey("Indexing.IndexMissingFieldsAsNull")
                    .containsValue("true");

        }
    }
}
