package net.ravendb.client.documents.dataArchival;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.dataArchival.ConfigureDataArchivalOperation;
import net.ravendb.client.documents.operations.dataArchival.DataArchivalConfiguration;
import net.ravendb.client.infrastructure.DisabledOnPullRequest;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisabledOnPullRequest
public class DataArchivalTest extends RemoteTestBase {

    @Test
    public void canSetupArchival() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            DataArchivalConfiguration configuration = new DataArchivalConfiguration();
            configuration.setArchiveFrequencyInSec(5L);
            configuration.setDisabled(true);

            store.maintenance().send(new ConfigureDataArchivalOperation(configuration));

            DatabaseRecordWithEtag databaseRecordWithEtag = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));

            assertThat(databaseRecordWithEtag.getDataArchival())
                    .isNotNull();
            assertThat(databaseRecordWithEtag.getDataArchival().isDisabled())
                    .isTrue();
            assertThat(databaseRecordWithEtag.getDataArchival().getArchiveFrequencyInSec())
                    .isEqualTo(5L);
        }
    }
}
