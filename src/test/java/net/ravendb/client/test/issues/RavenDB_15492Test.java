package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_15492Test extends RemoteTestBase {

    @Test
    public void willCallOnBeforeDeleteWhenCallingDeleteById() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                AtomicBoolean called = new AtomicBoolean(false);
                session.advanced().addBeforeDeleteListener((sender, event) -> called.set("users/1".equals(event.getDocumentId())));

                session.delete("users/1");
                session.saveChanges();

                assertThat(called.get())
                        .isTrue();
            }
        }
    }
}
