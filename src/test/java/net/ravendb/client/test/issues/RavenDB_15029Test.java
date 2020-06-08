package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesRawResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IRawDocumentQuery;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15029Test extends RemoteTestBase {

    @Test
    public void sessionRawQueryShouldNotTrackTimeSeriesResultAsDocument() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Karmel");
                session.store(user, "users/karmel");
                session.timeSeriesFor("users/karmel", "HeartRate")
                        .append(baseLine, 60, "watches/fitbit");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                User u = session.load(User.class, "users/karmel");
                IRawDocumentQuery<TimeSeriesRawResult> query = session.advanced().rawQuery(TimeSeriesRawResult.class,
                        "declare timeseries out()\n" +
                                "{\n" +
                                "    from HeartRate\n" +
                                "}\n" +
                                "from Users as u\n" +
                                "where name = 'Karmel'\n" +
                                "select out()");

                TimeSeriesRawResult result = query.first();

                assertThat(result.getCount())
                        .isEqualTo(1);
                assertThat(result.getResults()[0].getValue())
                        .isEqualTo(60.0);
                assertThat(result.getResults()[0].getTimestamp())
                        .isEqualTo(baseLine);
                assertThat(result.getResults()[0].getTag())
                        .isEqualTo("watches/fitbit");
            }
        }
    }
}
