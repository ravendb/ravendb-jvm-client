package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_10638Test extends RemoteTestBase {

    @Test
    public void afterQueryExecutedShouldBeExecutedOnlyOnce() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {

                AtomicInteger counter = new AtomicInteger();

                List<User> results = session.query(User.class)
                        .addAfterQueryExecutedListener(x -> {
                            counter.incrementAndGet();
                        })
                        .whereEquals("name", "Doe")
                        .toList();

                assertThat(results)
                        .isEmpty();
                assertThat(counter)
                        .hasValue(1);
            }
        }
    }

}
