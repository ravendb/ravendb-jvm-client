package net.ravendb.client.test.server;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.DisableDatabaseToggleResult;
import net.ravendb.client.documents.operations.ToggleDatabasesStateOperation;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.AddDatabaseNodeOperation;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DatabasePutResult;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DatabasesTest extends RemoteTestBase {

    @Test
    public void canDisableAndEnableDatabase() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            DatabaseRecord dbRecord = new DatabaseRecord("enableDisable");
            CreateDatabaseOperation databaseOperation = new CreateDatabaseOperation(dbRecord);
            store.maintenance().server().send(databaseOperation);

            DisableDatabaseToggleResult toggleResult = store.maintenance().server().send(
                    new ToggleDatabasesStateOperation("enableDisable", true));

            assertThat(toggleResult)
                    .isNotNull();
            assertThat(toggleResult.getName())
                    .isNotNull();

            DatabaseRecordWithEtag disabledDatabaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation("enableDisable"));

            assertThat(disabledDatabaseRecord.isDisabled())
                    .isTrue();

            // now enable database

            toggleResult = store.maintenance().server().send(
                    new ToggleDatabasesStateOperation("enableDisable", false));
            assertThat(toggleResult)
                    .isNotNull();
            assertThat(toggleResult.getName())
                    .isNotNull();

            DatabaseRecordWithEtag enabledDatabaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation("enableDisable"));

            assertThat(enabledDatabaseRecord.isDisabled())
                    .isFalse();
        }
    }

    @Test
    public void canAddNode() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            assertThatThrownBy(() -> {
                // we assert this by throwing - we are running single node cluster
                DatabasePutResult send = store.maintenance().server().send(new AddDatabaseNodeOperation(store.getDatabase()));
            }).hasMessageContaining("Can't add node");
        }
    }
}
