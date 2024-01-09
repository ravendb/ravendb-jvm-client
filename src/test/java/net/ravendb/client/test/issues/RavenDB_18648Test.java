package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.EssentialDatabaseStatistics;
import net.ravendb.client.documents.operations.EssentialIndexInformation;
import net.ravendb.client.documents.operations.GetEssentialStatisticsOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.orders.Company;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_18648Test extends RemoteTestBase {

    @Test
    public void can_Get_Essential_Database_Statistics() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            EssentialDatabaseStatistics stats = store.maintenance().send(new GetEssentialStatisticsOperation());

            assertThat(stats.getCountOfAttachments())
                    .isZero();
            assertThat(stats.getCountOfConflicts())
                    .isZero();
            assertThat(stats.getCountOfCounterEntries())
                    .isZero();
            assertThat(stats.getCountOfDocuments())
                    .isZero();
            assertThat(stats.getCountOfDocumentsConflicts())
                    .isZero();
            assertThat(stats.getCountOfIndexes())
                    .isZero();
            assertThat(stats.getCountOfRevisionDocuments())
                    .isZero();
            assertThat(stats.getCountOfTimeSeriesSegments())
                    .isZero();
            assertThat(stats.getCountOfTombstones())
                    .isZero();
            assertThat(stats.getIndexes().length)
                    .isZero();

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 20; i++) {
                    Company company = new Company();
                    session.store(company);
                    session.timeSeriesFor(company, "TS")
                            .append(new Date(), 1);
                    session.countersFor(company)
                            .increment("CTR", 1);
                    session.advanced().attachments().store(company, "a1", new ByteArrayInputStream(new byte[0]));
                }

                session.saveChanges();
            }

            RavenDB_9745Test.Companies_ByName index = new RavenDB_9745Test.Companies_ByName();
            index.execute(store);

            stats = store.maintenance().send(new GetEssentialStatisticsOperation());

            assertThat(stats.getCountOfAttachments())
                    .isEqualTo(20);
            assertThat(stats.getCountOfConflicts())
                    .isZero();
            assertThat(stats.getCountOfCounterEntries())
                    .isEqualTo(20);
            assertThat(stats.getCountOfDocuments())
                    .isEqualTo(21);
            assertThat(stats.getCountOfDocumentsConflicts())
                    .isEqualTo(0);
            assertThat(stats.getCountOfIndexes())
                    .isEqualTo(1);
            assertThat(stats.getCountOfRevisionDocuments())
                    .isEqualTo(0);
            assertThat(stats.getCountOfTimeSeriesSegments())
                    .isEqualTo(20);
            assertThat(stats.getCountOfTombstones())
                    .isEqualTo(0);
            assertThat(stats.getIndexes())
                    .hasSize(1);

            EssentialIndexInformation indexInformation = stats.getIndexes()[0];
            assertThat(indexInformation.getName())
                    .isEqualTo(index.getIndexName());

        }
    }
}
