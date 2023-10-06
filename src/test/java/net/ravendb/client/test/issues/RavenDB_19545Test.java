package net.ravendb.client.test.issues;

import net.ravendb.client.RavenTestHelper;
import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_19545Test extends RemoteTestBase {

    @Test
    public void removingTimeSeriesEntryShouldAffectCache() throws Exception {
        String docId = "user/1";
        String timeSeriesName = "HeartRates";
        String tag = "watches/fitbit";

        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Lev");
                session.store(user, docId);

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(docId, timeSeriesName);
                tsf.append(DateUtils.addHours(RavenTestHelper.utcToday(), 23), 67, tag);
                session.saveChanges();

                TimeSeriesEntry[] entries = session.timeSeriesFor(docId, timeSeriesName).get();
                assertThat(entries)
                        .hasSize(1);

                session.timeSeriesFor(docId, timeSeriesName).delete(null, null);
                session.saveChanges();

                TimeSeriesEntry[] entries2 = session.timeSeriesFor(docId, timeSeriesName).get();
                assertThat(entries2)
                        .isNull();

            }
        }
    }

    @Test
    public void removingTimeSeriesEntryShouldAffectCache2() throws Exception {
        String docId = "user/1";
        String timeSeriesName = "HeartRates";
        String tag = "watches/fitbit";
        Date baseline = RavenTestHelper.utcToday();

        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Lev");
                session.store(user, docId);

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(docId, timeSeriesName);
                for (int i = 1; i <= 10; i++) {
                    tsf.append(DateUtils.addDays(baseline, i), i, tag);
                    session.saveChanges();
                }

                TimeSeriesEntry[] entries = session.timeSeriesFor(docId, timeSeriesName)
                        .get(DateUtils.addDays(baseline, 9), DateUtils.addDays(baseline, 11));
                assertThat(entries)
                        .hasSize(1);

                entries = session.timeSeriesFor(docId, timeSeriesName)
                        .get(DateUtils.addDays(baseline, 3), DateUtils.addDays(baseline, 8));
                assertThat(entries)
                        .hasSize(5);

                session.timeSeriesFor(docId, timeSeriesName)
                                .delete(DateUtils.addDays(baseline, 4), DateUtils.addDays(baseline, 7));
                session.saveChanges();

                TimeSeriesEntry[] entries2 = session.timeSeriesFor(docId, timeSeriesName).get();
                assertThat(entries2)
                        .hasSize(6);
            }
        }
    }

    @Test
    public void removingTimeSeriesEntryShouldAffectCache3() throws Exception {
        String docId = "user/1";
        String timeSeriesName = "HeartRates";
        String tag = "watches/fitbit";
        Date baseline = RavenTestHelper.utcToday();

        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Lev");
                session.store(user, docId);

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(docId, timeSeriesName);
                for (int i = 1; i <= 10; i++) {
                    tsf.append(DateUtils.addDays(baseline, i), i, tag);
                    session.saveChanges();
                }

                TimeSeriesEntry[] entries = session.timeSeriesFor(docId, timeSeriesName)
                        .get(DateUtils.addSeconds(DateUtils.addDays(baseline, 9), 1), DateUtils.addDays(baseline, 11));

                assertThat(entries)
                        .hasSize(1);

                entries = session.timeSeriesFor(docId, timeSeriesName)
                        .get(null, DateUtils.addDays(baseline, 8));
                assertThat(entries)
                        .hasSize(8);

                session.timeSeriesFor(docId, timeSeriesName)
                        .delete(null, DateUtils.addDays(baseline, 7));
                session.saveChanges();

                TimeSeriesEntry[] entries2 = session.timeSeriesFor(docId, timeSeriesName).get();
                assertThat(entries2)
                        .hasSize(3);
            }
        }
    }

    @Test
    public void removingTimeSeriesEntryShouldAffectCache4() throws Exception {
        String docId = "user/1";
        String timeSeriesName = "HeartRates";
        String tag = "watches/fitbit";
        Date baseline = RavenTestHelper.utcToday();


        try (DocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Lev");
                session.store(user, docId);

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(docId, timeSeriesName);
                for (int i = 1; i <= 10; i++) {
                    tsf.append(DateUtils.addDays(baseline, i), i, tag);
                    session.saveChanges();
                }

                TimeSeriesEntry[] entries = session.timeSeriesFor(docId, timeSeriesName)
                        .get(DateUtils.addDays(baseline, 9), DateUtils.addDays(baseline, 11));
                assertThat(entries)
                        .hasSize(1);

                entries = session.timeSeriesFor(docId, timeSeriesName).get(DateUtils.addDays(baseline, 1), null);
                assertThat(entries)
                        .hasSize(9);

                session.timeSeriesFor(docId, timeSeriesName)
                        .delete(DateUtils.addDays(baseline, 6), null);
                session.saveChanges();

                TimeSeriesEntry[] entries2 = session.timeSeriesFor(docId, timeSeriesName)
                        .get();
                assertThat(entries2)
                        .hasSize(5);
            }
        }
    }
}
