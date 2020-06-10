package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.timeSeries.GetTimeSeriesOperation;
import net.ravendb.client.documents.operations.timeSeries.TimeSeriesRangeResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_14994Test extends RemoteTestBase {

    @Test
    public void getOnNonExistingTimeSeriesShouldReturnNull() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);
                session.saveChanges();
            }

            TimeSeriesRangeResult get = store.operations().send(new GetTimeSeriesOperation(documentId, "HeartRate"));
            assertThat(get)
                    .isNull();

            try (IDocumentSession session = store.openSession()) {
                assertThat(session.timeSeriesFor(documentId, "HeartRate")
                        .get()).isNull();
            }
        }
    }

    @Test
    public void getOnEmptyRangeShouldReturnEmptyArray() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";

            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "HeartRate");
                for (int i = 0; i < 10; i++) {
                    tsf.append(DateUtils.addMinutes(baseLine, i), i);
                }

                session.saveChanges();
            }

            TimeSeriesRangeResult get = store.operations().send(new GetTimeSeriesOperation(documentId, "HeartRate", DateUtils.addMinutes(baseLine, -2), DateUtils.addMinutes(baseLine, -1)));
            assertThat(get.getEntries())
                    .isEmpty();

            try (IDocumentSession session = store.openSession()) {
                List<TimeSeriesEntry> result = Arrays.asList(session.timeSeriesFor(documentId, "HeartRate")
                        .get(DateUtils.addMonths(baseLine, -2), DateUtils.addMonths(baseLine, -1)));
                assertThat(result)
                        .isEmpty();
            }
        }
    }
}
