package net.ravendb.client.serverwide.operations.configuration;

import net.ravendb.client.documents.operations.backups.PeriodicBackupConfiguration;
import net.ravendb.client.serverwide.operations.ongoingTasks.IServerWideTask;

public class ServerWideBackupConfiguration extends PeriodicBackupConfiguration implements IServerWideTask {

    private String namePrefix = "Server Wide Backup";

    private String[] excludedDatabases;

    public String getNamePrefix() {
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String[] getExcludedDatabases() {
        return excludedDatabases;
    }

    public void setExcludedDatabases(String[] excludedDatabases) {
        this.excludedDatabases = excludedDatabases;
    }
}
