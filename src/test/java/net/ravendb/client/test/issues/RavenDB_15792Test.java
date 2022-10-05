package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.queries.timeSeries.TimeSeriesRawResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IRawDocumentQuery;
import net.ravendb.client.documents.session.ISessionDocumentTimeSeries;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15792Test extends RemoteTestBase {

    @Test
    public void canQueryTimeSeriesWithSpacesInName() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String documentId = "users/ayende";

            Date baseLine = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), documentId);

                ISessionDocumentTimeSeries tsf = session.timeSeriesFor(documentId, "gas m3 usage");
                tsf.append(baseLine, 1);

                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                IRawDocumentQuery<TimeSeriesRawResult> query = session.advanced().rawQuery(TimeSeriesRawResult.class,
                        "declare timeseries out()\n" +
                                "{\n" +
                                "    from \"gas m3 usage\"\n" +
                                "}\n" +
                                "from Users as u\n" +
                                "select out()");

                TimeSeriesRawResult result = query.first();
                assertThat(result)
                        .isNotNull();

                TimeSeriesEntry[] results = result.getResults();

                assertThat(results)
                        .hasSize(1);
            }
        }
    }
}
