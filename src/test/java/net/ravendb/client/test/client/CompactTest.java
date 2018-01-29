package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.CompactDatabaseOperation;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.serverwide.CompactSettings;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class CompactTest extends RemoteTestBase {

    @Test
    public void canCompactDatabase() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {

            try (IDocumentSession newSession = store.openSession()) {
                User user1 = new User();
                user1.setLastName("user1");
                newSession.store(user1, "users/1");
                newSession.saveChanges();
            }

            CompactSettings compactSettings = new CompactSettings();
            compactSettings.setDatabaseName(store.getDatabase());
            compactSettings.setDocuments(true);

            Operation operation = store.maintenance().server().sendAsync(new CompactDatabaseOperation(compactSettings));

            // we can't compact in memory database but here we just test is request was send successfully
            Assertions.assertThatThrownBy(() -> operation.waitForCompletion())
                    .hasMessageContaining("Unable to cast object of type 'PureMemoryStorageEnvironmentOptions' to type 'DirectoryStorageEnvironmentOptions'");
        }
    }
}
