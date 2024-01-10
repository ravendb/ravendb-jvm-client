package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.CloseableIterator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.StreamResult;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import org.apache.hc.core5.http.ConnectionRequestTimeoutException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RDBC_323Test extends RemoteTestBase {

    private final int streamsToOpen = 40;

    @Test
    @Disabled("slow test - with leaks simulation")
    public void throwsOnConnectionPoolStarvation() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            List<CloseableIterator<StreamResult<User>>> streamIterators = new ArrayList<>();

            assertThatThrownBy(() -> {
                try {
                    for (int i = 0; i < streamsToOpen; i++) {
                        try (IDocumentSession session = store.openSession()) {
                            CloseableIterator<StreamResult<User>> userStream = session.advanced().stream(User.class, "user");
                            streamIterators.add(userStream);
                        }
                    }
                } finally {
                    streamIterators.forEach(x -> x.close());
                }
            }).hasCauseExactlyInstanceOf(ConnectionRequestTimeoutException.class);
        }
    }
}
