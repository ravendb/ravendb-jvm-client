package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_21993Test extends RemoteTestBase {

    @Test
    public void changesShouldCleanupFaultyConnectionByItself() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IDatabaseChanges changes = store.changes("no_such_db");

            assertThatThrownBy(changes::ensureConnectedNow)
                    .isExactlyInstanceOf(DatabaseDoesNotExistException.class);

            // the faulted changes object disposes itself and is removed from the store
            Boolean removed = waitForValue(() -> store.changes("no_such_db") != changes, true);
            assertThat(removed)
                    .isTrue();
        }
    }
}
