package net.ravendb.client.serverwide.operations.configuration;

import net.ravendb.client.documents.operations.backups.PeriodicBackupConfiguration;

public class ServerWideBackupConfiguration extends PeriodicBackupConfiguration {

    private String namePrefix = "Server Wide Backup";

    public String getNamePrefix() {
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }
}
