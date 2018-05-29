package net.ravendb.client.serverwide.commands;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GetDatabaseRecordTest extends RemoteTestBase {

    @Test
    public void canGetDatabaseRecord() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            DatabaseRecordWithEtag databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));

            assertThat(databaseRecord)
                    .isNotNull();
            assertThat(databaseRecord.getDatabaseName())
                    .isEqualTo(store.getDatabase());
        }
    }
}
