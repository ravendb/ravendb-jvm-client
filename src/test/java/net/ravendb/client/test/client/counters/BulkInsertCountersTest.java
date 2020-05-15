package net.ravendb.client.test.client.counters;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.BulkInsertOperation;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.counters.CounterDetail;
import net.ravendb.client.documents.operations.counters.CountersDetail;
import net.ravendb.client.documents.operations.counters.GetCountersOperation;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BulkInsertCountersTest extends RemoteTestBase {

    @Test
    public void incrementCounter() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String userId1;
            String userId2;

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user1 = new User();
                user1.setName("Aviv1");
                bulkInsert.store(user1);
                userId1 = user1.getId();

                User user2 = new User();
                user2.setName("Aviv2");
                bulkInsert.store(user2);
                userId2 = user2.getId();

                BulkInsertOperation.CountersBulkInsert counter = bulkInsert.countersFor(userId1);

                counter.increment("likes", 100);
                counter.increment("downloads", 500);
                bulkInsert.countersFor(userId2)
                        .increment("votes", 1000);
            }

            List<CounterDetail> counters = store.operations()
                    .send(new GetCountersOperation(userId1, new String[]{"likes", "downloads"}))
                    .getCounters();

            assertThat(counters)
                    .hasSize(2);

            counters.sort(Comparator.comparing(CounterDetail::getCounterName));

            assertThat(counters.get(0).getTotalValue())
                    .isEqualTo(500);

            assertThat(counters.get(1).getTotalValue())
                    .isEqualTo(100);


            CountersDetail val = store.operations()
                    .send(new GetCountersOperation(userId2, "votes"));

            assertThat(val.getCounters().get(0).getTotalValue())
                    .isEqualTo(1000);
        }
    }

    @Test
    public void addDocumentAfterIncrementCounter() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String userId1;
            String userId2;

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user1 = new User();
                user1.setName("Grisha");
                bulkInsert.store(user1);

                userId1 = user1.getId();
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                BulkInsertOperation.CountersBulkInsert counter = bulkInsert.countersFor(userId1);

                counter.increment("likes", 100);
                counter.increment("downloads", 500);

                User user2 = new User();
                user2.setName("Kotler");
                bulkInsert.store(user2);

                userId2 = user2.getId();

                bulkInsert.countersFor(userId2)
                        .increment("votes", 1000);
            }

            List<CounterDetail> counters = store.operations()
                    .send(new GetCountersOperation(userId1, new String[]{"likes", "downloads"}))
                    .getCounters();

            assertThat(counters)
                    .hasSize(2);

            counters.sort(Comparator.comparing(CounterDetail::getCounterName));

            assertThat(counters.get(0).getTotalValue())
                    .isEqualTo(500);

            assertThat(counters.get(1).getTotalValue())
                    .isEqualTo(100);


            CountersDetail val = store.operations()
                    .send(new GetCountersOperation(userId2, "votes"));

            assertThat(val.getCounters().get(0).getTotalValue())
                    .isEqualTo(1000);
        }
    }

    @Test
    public void incrementCounterInSeparateBulkInserts() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String userId1;
            String userId2;

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user1 = new User();
                user1.setName("Aviv1");
                bulkInsert.store(user1);
                userId1 = user1.getId();

                User user2 = new User();
                user2.setName("Aviv2");
                bulkInsert.store(user2);
                userId2 = user2.getId();
            }

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                BulkInsertOperation.CountersBulkInsert counter = bulkInsert.countersFor(userId1);
                counter.increment("likes", 100);
                bulkInsert.countersFor(userId2)
                        .increment("votes", 1000);
                counter.increment("downloads", 500);
            }

            List<CounterDetail> counters = store.operations()
                    .send(new GetCountersOperation(userId1, new String[]{"likes", "downloads"}))
                    .getCounters();

            assertThat(counters)
                    .hasSize(2);

            counters.sort(Comparator.comparing(CounterDetail::getCounterName));

            assertThat(counters.get(0).getTotalValue())
                    .isEqualTo(500);

            assertThat(counters.get(1).getTotalValue())
                    .isEqualTo(100);


            CountersDetail val = store.operations()
                    .send(new GetCountersOperation(userId2, "votes"));

            assertThat(val.getCounters().get(0).getTotalValue())
                    .isEqualTo(1000);
        }
    }

    @Test
    public void incrementCounterNullId() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> {
                try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                    bulkInsert.countersFor(null)
                            .increment("votes", 1000);
                }
            }).hasMessageContaining("Document id cannot be null or empty");
        }
    }

    @Test
    public void incrementManyCounters() throws Exception {
        int counterCount = 20_000;

        try (IDocumentStore store = getDocumentStore()) {
            String userId1;

            try (BulkInsertOperation bulkInsert = store.bulkInsert()) {
                User user1 = new User();
                user1.setName("Aviv1");
                bulkInsert.store(user1);
                userId1 = user1.getId();

                BulkInsertOperation.CountersBulkInsert counter = bulkInsert.countersFor(userId1);

                for (int i = 1; i < counterCount + 1; i++) {
                    counter.increment(String.valueOf(i), i);
                }
            }

            List<CounterDetail> counters = store.operations()
                    .send(new GetCountersOperation(userId1))
                    .getCounters();

            assertThat(counters)
                    .hasSize(counterCount);

            for (CounterDetail counter : counters) {
                assertThat(counter.getTotalValue())
                        .isEqualTo(Long.valueOf(counter.getCounterName()));
            }
        }
    }
}
