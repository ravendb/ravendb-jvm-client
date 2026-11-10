package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.counters.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.ISessionDocumentCounters;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_22150Test extends RemoteTestBase {

    @Test
    public void incrementByDefaultValue() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                user.setName("Danielle");
                session.store(user, "users/1");
                session.saveChanges();
            }

            try (IDocumentSession session = store.openSession()) {
                ISessionDocumentCounters documentCounters = session.countersFor("users/1");
                documentCounters.increment("Likes", 10);
                documentCounters.increment("Dislikes", 20);
                documentCounters.increment("Downloads", 30);
                session.saveChanges();
            }

            // increment by 5
            CounterOperation likes = new CounterOperation();
            likes.setType(CounterOperationType.INCREMENT);
            likes.setCounterName("likes");
            likes.setDelta(5);

            // should increment by 1 (default value)
            CounterOperation dislikes = new CounterOperation();
            dislikes.setType(CounterOperationType.INCREMENT);
            dislikes.setCounterName("dislikes");

            DocumentCountersOperation documentCountersOperation = new DocumentCountersOperation();
            documentCountersOperation.setDocumentId("users/1");
            documentCountersOperation.setOperations(Arrays.asList(likes, dislikes));

            CounterBatch counterBatch = new CounterBatch();
            counterBatch.setDocuments(Collections.singletonList(documentCountersOperation));

            CountersDetail operationResult = store.operations().send(new CounterBatchOperation(counterBatch));

            assertThat(operationResult.getCounters().get(1).getTotalValue()).isEqualTo(21);
        }
    }
}
