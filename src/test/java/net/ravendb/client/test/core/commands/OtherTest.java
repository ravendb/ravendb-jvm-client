package net.ravendb.client.test.core.commands;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.DatabaseStatistics;
import net.ravendb.client.documents.operations.DetailedDatabaseStatistics;
import net.ravendb.client.documents.operations.GetDetailedStatisticsOperation;
import net.ravendb.client.documents.operations.GetStatisticsOperation;
import net.ravendb.client.documents.operations.compareExchange.PutCompareExchangeValueOperation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentCounters;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class OtherTest extends RemoteTestBase {

    @Test
    public void canGetDatabaseStatistics() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 10; i++) {
                    String id = "foo/bar/" + i;
                    User user = new User();
                    user.setName("Original shard");
                    session.store(user, id);
                    session.saveChanges();

                    Date baseline = RavenTestHelper.utcToday();
                    ISessionDocumentTimeSeries ts = session.timeSeriesFor(id, "HeartRates");
                    ISessionDocumentCounters cf = session.countersFor(id);
                    for (int j = 0; j < 20; j++) {
                        ts.append(DateUtils.addMinutes(baseline, j), j, "watches/apple");
                        cf.increment("Likes", j);
                    }

                    session.saveChanges();
                }
            }

            DatabaseStatistics databaseStatistics = store.maintenance().send(new GetStatisticsOperation());
            DetailedDatabaseStatistics detailedDatabaseStatistics = store.maintenance().send(new GetDetailedStatisticsOperation());

            assertThat(databaseStatistics)
                    .isNotNull();
            assertThat(detailedDatabaseStatistics)
                    .isNotNull();

            assertThat(databaseStatistics.getCountOfDocuments())
                    .isEqualTo(10);
            assertThat(databaseStatistics.getCountOfCounterEntries())
                    .isEqualTo(10);
            assertThat(databaseStatistics.getCountOfTimeSeriesSegments())
                    .isEqualTo(10);

            assertThat(detailedDatabaseStatistics.getCountOfDocuments())
                    .isEqualTo(10);
            assertThat(detailedDatabaseStatistics.getCountOfCounterEntries())
                    .isEqualTo(10);
            assertThat(detailedDatabaseStatistics.getCountOfTimeSeriesSegments())
                    .isEqualTo(10);

            try (IDocumentSession session = store.openSession()) {
                session.timeSeriesFor("foo/bar/0", "HeartRates").delete();
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                session.delete("foo/bar/0");
                session.saveChanges();
            }

            store.operations().send(new PutCompareExchangeValueOperation<>("users/1", "Raven", 0));

            databaseStatistics = store.maintenance().send(new GetStatisticsOperation());
            detailedDatabaseStatistics = store.maintenance().send(new GetDetailedStatisticsOperation());

            assertThat(databaseStatistics.getCountOfDocuments())
                    .isEqualTo(9);
            assertThat(databaseStatistics.getCountOfTombstones())
                    .isEqualTo(1);

            assertThat(detailedDatabaseStatistics.getCountOfDocuments())
                    .isEqualTo(9);
            assertThat(detailedDatabaseStatistics.getCountOfTombstones())
                    .isEqualTo(1);
            assertThat(detailedDatabaseStatistics.getCountOfCompareExchange())
                    .isEqualTo(1);
            assertThat(detailedDatabaseStatistics.getCountOfTimeSeriesDeletedRanges())
                    .isEqualTo(1);
        }
    }
}
