package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_16906Test extends RemoteTestBase {

    @Test
    public void timeSeriesFor_ShouldThrowBetterError_OnNullEntity() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try (IDocumentSession session = store.openSession()) {
                User user = session.load(User.class, "users/1");
                assertThatThrownBy(() -> session.timeSeriesFor(user, "heartRate"))
                        .isExactlyInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Entity cannot be null");
            }
        }
    }
}
