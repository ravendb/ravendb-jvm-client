package net.ravendb.client.documents.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.operations.IndexInformation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.smuggler.DatabaseItemType;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.CreateSampleDataOperation;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

public class GetStatisticsCommandTest extends RemoteTestBase {

    @Test
    public void canGetStats() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            RequestExecutor executor = store.getRequestExecutor();

            CreateSampleDataOperation sampleData =
                    new CreateSampleDataOperation(
                            EnumSet.of(
                                    DatabaseItemType.DOCUMENTS,
                                    DatabaseItemType.INDEXES,
                                    DatabaseItemType.ATTACHMENTS,
                                    DatabaseItemType.REVISION_DOCUMENTS
                            ));
            store.maintenance().send(sampleData);

            waitForIndexing(store, store.getDatabase(), null);

            GetStatisticsOperation.GetStatisticsCommand command = new GetStatisticsOperation.GetStatisticsCommand();
            executor.execute(command);

            DatabaseStatistics stats = command.getResult();
            assertThat(stats)
                    .isNotNull();

            assertThat(stats.getLastDocEtag())
                    .isNotNull()
                    .isGreaterThan(0);

            assertThat(stats.getCountOfIndexes())
                    .isGreaterThanOrEqualTo(3);

            assertThat(stats.getCountOfDocuments())
                    .isEqualTo(1059);

            assertThat(stats.getCountOfRevisionDocuments())
                    .isGreaterThan(0);

            assertThat(stats.getCountOfDocumentsConflicts())
                    .isEqualTo(0);

            assertThat(stats.getCountOfConflicts())
                    .isEqualTo(0);

            assertThat(stats.getCountOfUniqueAttachments())
                    .isEqualTo(17);

            assertThat(stats.getDatabaseChangeVector())
                    .isNotEmpty();

            assertThat(stats.getDatabaseId())
                    .isNotEmpty();

            assertThat(stats.getPager())
                    .isNotEmpty();

            assertThat(stats.getLastIndexingTime())
                    .isNotNull();

            assertThat(stats.getIndexes())
                    .isNotNull();

            assertThat(stats.getSizeOnDisk().getHumaneSize())
                    .isNotNull();

            assertThat(stats.getSizeOnDisk().getSizeInBytes())
                    .isNotNull();

            for (IndexInformation indexInformation : stats.getIndexes()) {
                assertThat(indexInformation.getName())
                        .isNotNull();

                assertThat(indexInformation.isStale())
                        .isFalse();

                assertThat(indexInformation.getState())
                        .isNotNull();

                assertThat(indexInformation.getLockMode())
                        .isNotNull();

                assertThat(indexInformation.getPriority())
                        .isNotNull();

                assertThat(indexInformation.getType())
                        .isNotNull();

                assertThat(indexInformation.getLastIndexingTime())
                        .isNotNull();
            }
        }
    }

    @Test
    public void canGetStatsForCountersAndTimeSeries() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/1");

                session.countersFor("users/1")
                        .increment("c1");

                session.countersFor("users/1")
                        .increment("c2");

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor("users/1", "Heartrate");
                tsf
                        .append(new Date(), 70);

                tsf
                        .append(DateUtils.addMinutes(new Date(), 1), 20);

                session.saveChanges();
            }

            DatabaseStatistics statistics = store.maintenance().send(new GetStatisticsOperation());

            assertThat(statistics.getCountOfCounterEntries())
                    .isEqualTo(1);
            assertThat(statistics.getCountOfTimeSeriesSegments())
                    .isEqualTo(1);
        }
    }
}
