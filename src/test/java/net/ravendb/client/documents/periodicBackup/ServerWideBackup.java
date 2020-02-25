package net.ravendb.client.documents.periodicBackup;

import net.ravendb.client.RemoteTestBase;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.operations.backups.AzureSettings;
import net.ravendb.client.documents.operations.backups.BackupType;
import net.ravendb.client.documents.operations.backups.FtpSettings;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.GetDatabaseRecordOperation;
import net.ravendb.client.serverwide.operations.configuration.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerWideBackup extends RemoteTestBase {

    @Test
    public void canCrudServerWideBackup() throws Exception {
        try (IDocumentStore store = getDocumentStore()) {
            try {

                ServerWideBackupConfiguration putConfiguration = new ServerWideBackupConfiguration();
                putConfiguration.setDisabled(true);
                putConfiguration.setFullBackupFrequency("0 2 * * 0");
                putConfiguration.setIncrementalBackupFrequency("0 2 * * 1");

                store.maintenance().server().send(new PutServerWideBackupConfigurationOperation(putConfiguration));

                FtpSettings ftpSettings = new FtpSettings();
                ftpSettings.setUrl("http://url:8080");
                ftpSettings.setDisabled(true);

                putConfiguration.setFtpSettings(ftpSettings);
                store.maintenance().server().send(new PutServerWideBackupConfigurationOperation(putConfiguration));

                AzureSettings azureSettings = new AzureSettings();
                azureSettings.setDisabled(true);
                azureSettings.setAccountKey("test");

                putConfiguration.setAzureSettings(azureSettings);
                store.maintenance().server().send(new PutServerWideBackupConfigurationOperation(putConfiguration));

                ServerWideBackupConfiguration[] serverWideBackups =
                        store.maintenance().server().send(new GetServerWideBackupConfigurationsOperation());

                assertThat(serverWideBackups)
                        .hasSize(3);

                DatabaseRecordWithEtag databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
                assertThat(databaseRecord.getPeriodicBackups())
                        .hasSize(3);

                // update one of the tasks
                ServerWideBackupConfiguration toUpdate = serverWideBackups[1];
                toUpdate.setBackupType(BackupType.SNAPSHOT);
                store.maintenance().server().send(new PutServerWideBackupConfigurationOperation(toUpdate));

                serverWideBackups = store.maintenance().server().send(new GetServerWideBackupConfigurationsOperation());
                assertThat(serverWideBackups)
                        .hasSize(3);

                databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(store.getDatabase()));
                assertThat(databaseRecord.getPeriodicBackups())
                        .hasSize(3);

                // new database includes all server-wide backups
                String newDbName = store.getDatabase() + "-testDatabase";
                store.maintenance().server().send(new CreateDatabaseOperation(new DatabaseRecord(newDbName)));
                databaseRecord = store.maintenance().server().send(new GetDatabaseRecordOperation(newDbName));
                assertThat(databaseRecord.getPeriodicBackups())
                        .hasSize(3);

                // get by name

                ServerWideBackupConfiguration backupConfiguration = store.maintenance().server()
                        .send(new GetServerWideBackupConfigurationOperation("Backup w/o destinations"));

                assertThat(backupConfiguration)
                        .isNotNull();
            } finally {
                cleanupServerWideBackups(store);
            }
        }
    }

    private void cleanupServerWideBackups(IDocumentStore store) {
        ServerWideBackupConfiguration[] backupConfigurations = store.maintenance().server().send(new GetServerWideBackupConfigurationsOperation());
        List<String> names = Arrays.stream(backupConfigurations)
                .map(x -> x.getName())
                .collect(Collectors.toList());

        for (String name : names) {
            store.maintenance().server().send(new DeleteServerWideBackupConfigurationOperation(name));
        }
    }
}
