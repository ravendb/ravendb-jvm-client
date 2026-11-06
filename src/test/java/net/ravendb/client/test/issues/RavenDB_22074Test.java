package net.ravendb.client.test.issues;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.dataArchival.ConfigureDataArchivalOperation;
import net.ravendb.client.documents.operations.dataArchival.DataArchivalConfiguration;
import net.ravendb.client.documents.operations.expiration.ConfigureExpirationOperation;
import net.ravendb.client.documents.operations.expiration.ExpirationConfiguration;
import net.ravendb.client.documents.operations.refresh.ConfigureRefreshOperation;
import net.ravendb.client.documents.operations.refresh.RefreshConfiguration;
import net.ravendb.client.infrastructure.EnableOnServer;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@EnableOnServer(thresholdVersion = "6.0")
public class RavenDB_22074Test extends RemoteTestBase {

    // C# ExpirationWithMaxItemsToProcessConfiguredShouldWork / RefreshWithMaxItemsToProcessConfiguredShouldWork drive the
    // server-side cleaner with a mocked clock, so only the configuration round-trip is client-observable

    @Test
    public void canSetMaxItemsToProcessOnExpirationRefreshAndDataArchival() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            ExpirationConfiguration expiration = new ExpirationConfiguration();
            expiration.setDisabled(false);
            expiration.setDeleteFrequencyInSec(600L);
            expiration.setMaxItemsToProcess(9L);
            store.maintenance().send(new ConfigureExpirationOperation(expiration));

            RefreshConfiguration refresh = new RefreshConfiguration();
            refresh.setDisabled(false);
            refresh.setRefreshFrequencyInSec(600L);
            refresh.setMaxItemsToProcess(10L);
            store.maintenance().send(new ConfigureRefreshOperation(refresh));

            DataArchivalConfiguration dataArchival = new DataArchivalConfiguration();
            dataArchival.setDisabled(false);
            dataArchival.setArchiveFrequencyInSec(600L);
            dataArchival.setMaxItemsToProcess(11L);
            store.maintenance().send(new ConfigureDataArchivalOperation(dataArchival));

            DatabaseRecordWithEtag record = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));

            assertThat(record.getExpiration().getMaxItemsToProcess())
                    .isEqualTo(9L);
            assertThat(record.getRefresh().getMaxItemsToProcess())
                    .isEqualTo(10L);
            assertThat(record.getDataArchival().getMaxItemsToProcess())
                    .isEqualTo(11L);
        }
    }
}
