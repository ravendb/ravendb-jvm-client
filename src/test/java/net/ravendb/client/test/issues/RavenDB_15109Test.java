package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.timeSeries.TimeSeriesEntry;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15109Test extends RemoteTestBase {

    @Test
    public void bulkIncrementNewCounterShouldAddCounterNameToMetadata() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id;

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Aviv1");
                bulkInsert.store(user);

                id = user.getId();

                BulkInsertOperation.CountersBulkInsert counter = bulkInsert.countersFor(id);
                for (int i = 1; i <= 10; i++) {
                    counter.increment(String.valueOf(i), i);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                Map<String, Long> all =
                        session.countersFor(id).getAll();
                assertThat(all)
                        .hasSize(10);
            }

            try (IDocumentSession session = store.openSession()) {
                User u = session.load(User.class, id);
                List<String> counters = session.advanced().getCountersFor(u);
                assertThat(counters)
                        .isNotNull()
                        .hasSize(10);
            }
        }
    }

    @Test
    public void bulkIncrementNewTimeSeriesShouldAddTimeSeriesNameToMetadata() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String id;
            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user = new User();
                user.setName("Aviv1");
                bulkInsert.store(user);

                id = user.getId();

                for (int i = 1; i <= 10; i++) {
                    try (BulkInsertOperation.TimeSeriesBulkInsert timeSeries = bulkInsert.timeSeriesFor(id, String.valueOf(i))) {
                        timeSeries.append(new Date(), i);
                    }
                }
            }

            try (IDocumentSession session = store.openSession()) {
                for (int i = 1; i <= 10; i++) {
                    TimeSeriesEntry[] all = session.timeSeriesFor(id, String.valueOf(i))
                            .get();
                    assertThat(all)
                            .hasSize(1);
                }
            }

            try (IDocumentSession session = store.openSession()) {
                User u = session.load(User.class, id);
                List<String> timeSeries = session.advanced().getTimeSeriesFor(u);
                assertThat(timeSeries)
                        .isNotNull()
                        .hasSize(10);
            }
        }
    }
}
