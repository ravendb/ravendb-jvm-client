package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_16985Test extends RemoteTestBase {

    @Test
    public void checkIfHasChangesIsTrueAfterAddingAttachment() throws Exception {
        try (DocumentStore store = getDocumentStore()) {

            try (IDocumentSession session = store.openSession()) {
                User user = new User();
                session.store(user);
                session.saveChanges();

                ByteArrayInputStream bais1 = new ByteArrayInputStream("my test text".getBytes());
                session.advanced().attachments().store(user, "my-test.txt", bais1);

                boolean hasChanges = session.advanced().hasChanges();
                assertThat(hasChanges)
                        .isTrue();
            }
        }
    }
}
