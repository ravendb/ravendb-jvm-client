package net.ravendb.client.test.core.streaming;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.StreamResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DocumentStreaming extends RemoteTestBase {
    @Test
    public void canStreamDocumentsStartingWith() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 200; i++) {
                    session.store(new User());
                }

                session.saveChanges();
            }

            int count = 0;

            try (IDocumentSession session = store.openSession()) {
                try (CloseableIterator<StreamResult<User>> reader = session.advanced().stream(User.class, "users/")) {
                    while (reader.hasNext()) {
                        count++;
                        User user = reader.next().getDocument();
                        assertThat(user)
                                .isNotNull();
                    }
                }
            }

            assertThat(count)
                    .isEqualTo(200);
        }
    }

    @SuppressWarnings({"SpellCheckingInspection", "EmptyTryBlock"})
    @Test
    public void streamWithoutIterationDoesntLeakConnection() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                for (int i = 0; i < 200; i++) {
                    session.store(new User());
                }

                session.saveChanges();
            }

            for (int i = 0; i < 5; i++) {
                try (IDocumentSession session = store.openSession()) {

                    try (CloseableIterator<StreamResult<User>> reader = session.advanced().stream(User.class, "users/")) {
                        // don't iterate
                    }
                }
            }
        }
    }
}
