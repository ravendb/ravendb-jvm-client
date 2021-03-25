package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.revisions.ConfigureRevisionsOperation;
import net.ravendb.client.documents.operations.revisions.RevisionsCollectionConfiguration;
import net.ravendb.client.documents.operations.revisions.RevisionsConfiguration;
import net.ravendb.client.infrastructure.orders.Company;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14881Test extends RemoteTestBase {

    @Test
    public void can_get_detailed_collection_statistics() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            RevisionsConfiguration configuration = new RevisionsConfiguration();
            configuration.setCollections(new HashMap<>());

            RevisionsCollectionConfiguration revisionsCollectionConfiguration = new RevisionsCollectionConfiguration();
            revisionsCollectionConfiguration.setDisabled(false);
            configuration.getCollections().put("Companies", revisionsCollectionConfiguration);


            store.maintenance().send(new ConfigureRevisionsOperation(configuration));

            // insert sample data
            try (BulkInsertOperation bulk = store.bulkInsert()) {
                for (int i = 0; i < 20; i++) {
                    Company company = new Company();
                    company.setId("company/" + i);
                    company.setName("name" + i);
                    bulk.store(company);
                }
            }

            // get detailed collection statistics before we are going to change some data
            // right now there shouldn't be any revisions

            DetailedCollectionStatistics detailedCollectionStatistics = store.maintenance().send(new GetDetailedCollectionStatisticsOperation());

            assertThat(detailedCollectionStatistics.getCountOfDocuments())
                    .isEqualTo(20);
            assertThat(detailedCollectionStatistics.getCountOfConflicts())
                    .isZero();

            assertThat(detailedCollectionStatistics.getCollections())
                    .hasSize(1);

            CollectionDetails companies = detailedCollectionStatistics.getCollections().get("Companies");
            assertThat(companies)
                    .isNotNull();

            assertThat(companies.getCountOfDocuments())
                    .isEqualTo(20);
            assertThat(companies.getSize().getSizeInBytes())
                    .isPositive();
        }
    }
}
