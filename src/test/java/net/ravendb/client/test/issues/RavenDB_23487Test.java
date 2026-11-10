package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.Operation;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.smuggler.DatabaseSmugglerExportOptions;
import net.ravendb.client.documents.smuggler.DatabaseSmugglerImportOptions;
import net.ravendb.client.infrastructure.entities.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class RavenDB_23487Test extends RemoteTestBase {

    @Test
    public void exportToStream_CanBeImportedIntoAnotherDatabase() throws Exception {
        try (IDocumentStore src = getDocumentStore();
             IDocumentStore dest = getDocumentStore()) {
            try (IDocumentSession session = src.openSession()) {
                User user = new User();
                user.setName("Bob");
                session.store(user, "users/1");
                session.saveChanges();
            }

            AtomicBoolean closed = new AtomicBoolean();
            ByteArrayOutputStream ms = new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    closed.set(true);
                    super.close();
                }
            };

            Operation exportOp = src.smuggler().exportAsync(new DatabaseSmugglerExportOptions(), ms);
            exportOp.waitForCompletion();

            // the caller's stream is not closed by the export
            assertThat(closed.get()).isFalse();
            assertThat(ms.size()).isPositive();

            Operation importOp = dest.smuggler().importAsync(new DatabaseSmugglerImportOptions(), new ByteArrayInputStream(ms.toByteArray()));
            importOp.waitForCompletion();

            try (IDocumentSession session = dest.openSession()) {
                User imported = session.load(User.class, "users/1");
                assertThat(imported).isNotNull();
                assertThat(imported.getName()).isEqualTo("Bob");
            }
        }
    }
}
