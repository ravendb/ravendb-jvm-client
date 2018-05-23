package net.ravendb.client.documents.operations.revisions;

import java.time.Duration;

public class RevisionsCollectionConfiguration {
    private Long minimumRevisionsToKeep;

    private Duration minimumRevisionAgeToKeep;

    private boolean disabled;

    private boolean purgeOnDelete;

    public Long getMinimumRevisionsToKeep() {
        return minimumRevisionsToKeep;
    }

    public void setMinimumRevisionsToKeep(Long minimumRevisionsToKeep) {
        this.minimumRevisionsToKeep = minimumRevisionsToKeep;
    }

    public Duration getMinimumRevisionAgeToKeep() {
        return minimumRevisionAgeToKeep;
    }

    public void setMinimumRevisionAgeToKeep(Duration minimumRevisionAgeToKeep) {
        this.minimumRevisionAgeToKeep = minimumRevisionAgeToKeep;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isPurgeOnDelete() {
        return purgeOnDelete;
    }

    public void setPurgeOnDelete(boolean purgeOnDelete) {
        this.purgeOnDelete = purgeOnDelete;
    }
}