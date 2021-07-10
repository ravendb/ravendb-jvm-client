package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_16537Test extends RemoteTestBase {

    @Test
    public void can_Use_OnSessionDisposing_Event() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            AtomicInteger counter = new AtomicInteger();

            try (IDocumentSession session = store.openSession()) {
                session.advanced().addOnSessionClosingListener((sender, event) -> {
                    assertThat(sender)
                            .isSameAs(session);

                    counter.incrementAndGet();
                });
            }

            assertThat(counter.get())
                    .isEqualTo(1);

            store.addOnSessionClosingListener((sender, event) -> {
                counter.incrementAndGet();
            });

            try (IDocumentSession session = store.openSession()) {
            }

            assertThat(counter.get())
                    .isEqualTo(2);
        }
    }
}
