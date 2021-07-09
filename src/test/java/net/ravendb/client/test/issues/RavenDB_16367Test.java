package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.ToggleDatabasesStateOperation;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DeleteDatabaseResult;
import net.ravendb.client.serverwide.operations.DeleteDatabasesOperation;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.operations.ongoingTasks.SetDatabasesLockOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RavenDB_16367Test extends RemoteTestBase {

    @Test
    public void canLockDatabase() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String databaseName1 = store.getDatabase() + "_LockMode_1";

            assertThatThrownBy(() -> {
                SetDatabasesLockOperation lockOperation = new SetDatabasesLockOperation(databaseName1, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);
                store.maintenance().server().send(lockOperation);
            })
                    .isExactlyInstanceOf(DatabaseDoesNotExistException.class);

            store.maintenance().server().send(new CreateDatabaseOperation(new DatabaseRecord(databaseName1)));

            DatabaseRecordWithEtag databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(databaseName1));
            assertThat(databaseRecord.getLockMode())
                    .isEqualTo(DatabaseRecord.DatabaseLockMode.UNLOCK);

            store.maintenance().server().send(new SetDatabasesLockOperation(databaseName1, DatabaseRecord.DatabaseLockMode.UNLOCK));

            databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(databaseName1));
            assertThat(databaseRecord.getLockMode())
                    .isEqualTo(DatabaseRecord.DatabaseLockMode.UNLOCK);

            store.maintenance().server().send(new SetDatabasesLockOperation(databaseName1, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR));

            databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(databaseName1));
            assertThat(databaseRecord.getLockMode())
                    .isEqualTo(DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);

            assertThatThrownBy(() -> store.maintenance().server().send(new DeleteDatabasesOperation(databaseName1, true)))
                    .hasMessageContaining("cannot be deleted because of the set lock mode");

            store.maintenance().server().send(new SetDatabasesLockOperation(databaseName1, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_IGNORE));

            databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(databaseName1));
            assertThat(databaseRecord.getLockMode())
                    .isEqualTo(DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_IGNORE);

            DeleteDatabaseResult result = store.maintenance().server().send(new DeleteDatabasesOperation(databaseName1, true));
            assertThat(result.getRaftCommandIndex())
                    .isEqualTo(-1);

            databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(databaseName1));
            assertThat(databaseRecord)
                    .isNotNull();
        }
    }

    @Test
    public void canLockDatabase_Multiple() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String databaseName1 = store.getDatabase() + "_LockMode_1";
            String databaseName2 = store.getDatabase() + "_LockMode_2";
            String databaseName3 = store.getDatabase() + "_LockMode_3";

            String[] databases = new String[] { databaseName1, databaseName2, databaseName3 };

            assertThatThrownBy(() -> {
                SetDatabasesLockOperation.Parameters parameters = new SetDatabasesLockOperation.Parameters();
                parameters.setDatabaseNames(databases);
                parameters.setMode(DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);
                store.maintenance().server().send(new SetDatabasesLockOperation(parameters));
            }).isExactlyInstanceOf(DatabaseDoesNotExistException.class);

            store.maintenance().server().send(new CreateDatabaseOperation(new DatabaseRecord(databaseName1)));

            assertThatThrownBy(() -> {
                SetDatabasesLockOperation.Parameters parameters = new SetDatabasesLockOperation.Parameters();
                parameters.setDatabaseNames(databases);
                parameters.setMode(DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);
                store.maintenance().server().send(new SetDatabasesLockOperation(parameters));
            }).isExactlyInstanceOf(DatabaseDoesNotExistException.class);

            assertLockMode(store, databaseName1, DatabaseRecord.DatabaseLockMode.UNLOCK);

            store.maintenance().server().send(new CreateDatabaseOperation(new DatabaseRecord(databaseName2)));
            store.maintenance().server().send(new CreateDatabaseOperation(new DatabaseRecord(databaseName3)));

            assertLockMode(store, databaseName2, DatabaseRecord.DatabaseLockMode.UNLOCK);
            assertLockMode(store, databaseName3, DatabaseRecord.DatabaseLockMode.UNLOCK);

            SetDatabasesLockOperation.Parameters p2 = new SetDatabasesLockOperation.Parameters();
            p2.setDatabaseNames(databases);
            p2.setMode(DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);
            store.maintenance().server().send(new SetDatabasesLockOperation(p2));

            assertLockMode(store, databaseName1, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);
            assertLockMode(store, databaseName2, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);
            assertLockMode(store, databaseName3, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);

            store.maintenance().server().send(new SetDatabasesLockOperation(databaseName2, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_IGNORE));

            assertLockMode(store, databaseName1, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);
            assertLockMode(store, databaseName2, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_IGNORE);
            assertLockMode(store, databaseName3, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);

            p2.setDatabaseNames(databases);
            p2.setMode(DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_IGNORE);

            store.maintenance().server().send(new SetDatabasesLockOperation(p2));

            assertLockMode(store, databaseName1, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_IGNORE);
            assertLockMode(store, databaseName2, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_IGNORE);
            assertLockMode(store, databaseName3, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_IGNORE);


            p2.setDatabaseNames(databases);
            p2.setMode(DatabaseRecord.DatabaseLockMode.UNLOCK);

            store.maintenance().server().send(new SetDatabasesLockOperation(p2));

            store.maintenance().server().send(new DeleteDatabasesOperation(databaseName1, true));
            store.maintenance().server().send(new DeleteDatabasesOperation(databaseName2, true));
            store.maintenance().server().send(new DeleteDatabasesOperation(databaseName3, true));
        }
    }

    @Test
    public void canLockDatabase_Disabled() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            String databaseName = store.getDatabase() + "_LockMode_1";

            store.maintenance().server().send(new CreateDatabaseOperation(new DatabaseRecord(databaseName)));

            assertLockMode(store, databaseName, DatabaseRecord.DatabaseLockMode.UNLOCK);

            store.maintenance().server().send(new ToggleDatabasesStateOperation(databaseName, true));

            store.maintenance().server().send(new SetDatabasesLockOperation(databaseName, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR));

            assertLockMode(store, databaseName, DatabaseRecord.DatabaseLockMode.PREVENT_DELETES_ERROR);

            store.maintenance().server().send(new SetDatabasesLockOperation(databaseName, DatabaseRecord.DatabaseLockMode.UNLOCK));

            store.maintenance().server().send(new DeleteDatabasesOperation(databaseName, true));
        }
    }

    private static void assertLockMode(IDocumentStore store, String databaseName, DatabaseRecord.DatabaseLockMode mode) {
        DatabaseRecordWithEtag databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(databaseName));
        assertThat(databaseRecord.getLockMode())
                .isEqualTo(mode);
    }
}
