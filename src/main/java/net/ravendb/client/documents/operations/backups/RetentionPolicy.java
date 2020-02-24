package net.ravendb.client.documents.operations.backups;

import java.time.Duration;

public class RetentionPolicy {
    private boolean disabled;
    private Duration minimumBackupAgeToKeep;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Duration getMinimumBackupAgeToKeep() {
        return minimumBackupAgeToKeep;
    }

    public void setMinimumBackupAgeToKeep(Duration minimumBackupAgeToKeep) {
        this.minimumBackupAgeToKeep = minimumBackupAgeToKeep;
    }
}
