package net.ravendb.client.serverwide.operations.builder;

import net.ravendb.client.documents.operations.backups.PeriodicBackupConfiguration;

public interface IBackupConfigurationBuilder {
    IBackupConfigurationBuilder addPeriodicBackup(PeriodicBackupConfiguration configuration);
}
