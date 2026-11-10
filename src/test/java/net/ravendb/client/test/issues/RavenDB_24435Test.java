package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.changes.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.infrastructure.entities.User;
import net.ravendb.client.primitives.CleanCloseable;
import org.eclipse.jetty.util.BlockingArrayQueue;
import net.ravendb.client.documents.commands.GetNextOperationIdCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.infrastructure.EnableOnServer;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnableOnServer(thresholdVersion = "6.2")
public class RavenDB_24435Test extends RemoteTestBase {

    @Test
    public void shouldUnescapeDatabaseNameFromDatabaseMissingHeader() throws Exception {
        String databaseName = "database-møøse-" + UUID.randomUUID();

        DocumentConventions conventions = new DocumentConventions();

        try (DocumentStore store = getDocumentStore()) {
            try (RequestExecutor executor = RequestExecutor.create(store.getUrls(), databaseName, null, null, null, store.getExecutorService(), conventions)) {
                GetNextOperationIdCommand command = new GetNextOperationIdCommand();

                assertThatThrownBy(() -> executor.execute(command))
                        .isExactlyInstanceOf(DatabaseDoesNotExistException.class)
                        .hasMessage("Database '" + databaseName + "' does not exist.");
            }
        }
    }

    @Test
    public void changesShouldWorkWithNonAsciiDatabaseName() throws Exception {
        try (DocumentStore store = getDocumentStore("database-møøse")) {
            BlockingQueue<DocumentChange> changesList = new BlockingArrayQueue<>();

            IDatabaseChanges changes = store.changes();
            changes.ensureConnectedNow();

            IChangesObservable<DocumentChange> observable = changes.forDocument("users/1");
            try (CleanCloseable subscription = observable.subscribe(Observers.create(changesList::add))) {
                try (IDocumentSession session = store.openSession()) {
                    session.store(new User(), "users/1");
                    session.saveChanges();
                }

                DocumentChange documentChange = changesList.poll(10, TimeUnit.SECONDS);
                assertThat(documentChange)
                        .isNotNull();
                assertThat(documentChange.getId())
                        .isEqualTo("users/1");
                assertThat(documentChange.getType())
                        .isEqualTo(DocumentChangeTypes.PUT);
            }
        }
    }
}
