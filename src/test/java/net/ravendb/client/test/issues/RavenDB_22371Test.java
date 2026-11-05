package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.changes.*;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.RequestedNodeUnavailableException;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_22371Test extends RemoteTestBase {

    @Test
    @SuppressWarnings("deprecation")
    public void operationChangesRequireNodeTag() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            IDatabaseChanges changes = store.changes();

            assertThatThrownBy(() -> changes.forOperationId(1))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Changes API must be provided a node tag in order to track node-specific operations.");

            assertThatThrownBy(changes::forAllOperations)
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Changes API must be provided a node tag in order to track node-specific operations.");
        }
    }

    @Test
    public void singleNodeChangesRequireNodeTag() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> store.changes(store.getDatabase(), null))
                    .isExactlyInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> store.changes(store.getDatabase(), " "))
                    .isExactlyInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void canUseSingleNodeChanges() throws Exception {
        // adjusted from RavenDB_13293 (single node)
        try (IDocumentStore store = getDocumentStore()) {
            String nodeTag = store.getRequestExecutor().getPreferredNode().currentNode.getClusterTag();

            ISingleNodeDatabaseChanges databaseChanges = store.changes(store.getDatabase(), nodeTag);
            databaseChanges.ensureConnectedNow();

            BlockingQueue<DocumentChange> list = new LinkedBlockingQueue<>();
            databaseChanges.forDocument("users/1").subscribe(Observers.create(list::add));

            // operation tracking is allowed with a node tag
            databaseChanges.forAllOperations();

            try (IDocumentSession session = store.openSession()) {
                session.store(new User(), "users/1");
                session.saveChanges();
            }

            assertThat(list.poll(15, TimeUnit.SECONDS))
                    .isNotNull();

            ISingleNodeDatabaseChanges invalid = store.changes(store.getDatabase(), "XYZ");
            assertThatThrownBy(invalid::ensureConnectedNow)
                    .isExactlyInstanceOf(RequestedNodeUnavailableException.class);
        }
    }
}
