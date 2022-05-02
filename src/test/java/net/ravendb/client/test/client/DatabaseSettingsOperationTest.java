package net.ravendb.client.test.client;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.operations.ToggleDatabasesStateOperation;
import net.ravendb.client.serverwide.operations.configuration.DatabaseSettings;
import net.ravendb.client.serverwide.operations.configuration.GetDatabaseSettingsOperation;
import net.ravendb.client.serverwide.operations.configuration.PutDatabaseSettingsOperation;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseSettingsOperationTest extends RemoteTestBase {

    @Test
    public void checkIfConfigurationSettingsIsEmpty() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            checkIfOurValuesGotSaved(store, new HashMap<>());
        }
    }

    @Test
    public void changeSingleSettingKeyOnServer() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            String name = "Storage.PrefetchResetThresholdInGb";
            String value = "10";

            Map<String, String> settings = new HashMap<>();
            settings.put(name, value);
            putConfigurationSettings(store, settings);
            checkIfOurValuesGotSaved(store, settings);
        }
    }

    @Test
    public void changeMultipleSettingsKeysOnServer() throws Exception {
        try (DocumentStore store = getDocumentStore()) {
            Map<String, String> settings = new HashMap<>();
            settings.put("Storage.PrefetchResetThresholdInGb", "10");
            settings.put("Storage.TimeToSyncAfterFlushInSec", "35");
            settings.put("Tombstones.CleanupIntervalInMin", "10");

            putConfigurationSettings(store, settings);
            checkIfOurValuesGotSaved(store, settings);
        }
    }

    private void putConfigurationSettings(DocumentStore store, Map<String, String> settings) {
        store.maintenance().send(new PutDatabaseSettingsOperation(store.getDatabase(), settings));
        store.maintenance().server().send(new ToggleDatabasesStateOperation(store.getDatabase(), true));
        store.maintenance().server().send(new ToggleDatabasesStateOperation(store.getDatabase(), false));
    }

    private DatabaseSettings getConfigurationSettings(DocumentStore store) {
        DatabaseSettings settings = store.maintenance().send(new GetDatabaseSettingsOperation(store.getDatabase()));
        assertThat(settings)
                .isNotNull();
        return settings;
    }

    private void checkIfOurValuesGotSaved(DocumentStore store, Map<String, String> data) {
        DatabaseSettings settings = getConfigurationSettings(store);

        for (Map.Entry<String, String> item : data.entrySet()) {
            String configurationValue = settings.getSettings().get(item.getKey());
            assertThat(configurationValue)
                    .isNotNull();
            assertThat(configurationValue)
                    .isEqualTo(item.getValue());
        }
    }

}
