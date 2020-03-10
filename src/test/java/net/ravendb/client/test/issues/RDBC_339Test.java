package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

public class RDBC_339Test extends RemoteTestBase {

    @Test
    public void invalidAttachmentsFormat() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User u = new User();
                u.setName("John");
                session.store(u);

                session.advanced().attachments().store(u, "data", new ByteArrayInputStream(new byte[]{1, 2, 3}));
                session.saveChanges();

                User u2 = new User();
                u2.setName("Oz");
                session.store(u2);
                session.saveChanges();
            }
        }
    }
}
