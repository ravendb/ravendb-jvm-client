package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

public class HttpsTest extends RemoteTestBase {

    @Test
    public void canConnectWithCertificate() throws Exception {
        try (IDocumentStore store = getSecuredDocumentStore()) {
            try (IDocumentSession newSession = store.openSession()) {
                User user1 = new User();
                user1.setLastName("user1");
                newSession.store(user1, "users/1");
                newSession.saveChanges();
            }
        }
    }

}
