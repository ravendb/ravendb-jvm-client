package net.ravendb.client.documents.operations.revisions;

public final class RevisionsBinConfiguration {

    private boolean disabled;
    private int minimumEntriesAgeToKeepInMin = 30 * 24 * 60;
    private long cleanerFrequencyInSec = 5 * 60;

    /**
     * @return true if the revisions bin cleaner is disabled; otherwise, false.
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * @param disabled true if the revisions bin cleaner is disabled; otherwise, false.
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Minimum age of revisions-bin entries (deleted docs with revisions) to keep in the database.
     * The revisions-bin cleaner deletes the entries that are older than that.
     * When set to 0, all revisions bin entries will be deleted. The default value is 30 days.
     * @return minimum entries age to keep (in minutes)
     */
    public int getMinimumEntriesAgeToKeepInMin() {
        return minimumEntriesAgeToKeepInMin;
    }

    /**
     * Minimum age of revisions-bin entries (deleted docs with revisions) to keep in the database.
     * The revisions-bin cleaner deletes the entries that are older than that.
     * When set to 0, all revisions bin entries will be deleted. The default value is 30 days.
     * @param minimumEntriesAgeToKeepInMin minimum entries age to keep (in minutes)
     */
    public void setMinimumEntriesAgeToKeepInMin(int minimumEntriesAgeToKeepInMin) {
        this.minimumEntriesAgeToKeepInMin = minimumEntriesAgeToKeepInMin;
    }

    /**
     * Frequency (in seconds) at which the revisions bin cleaner executes cleaning. The default value is 5 minutes.
     * @return cleaner frequency (in seconds)
     */
    public long getCleanerFrequencyInSec() {
        return cleanerFrequencyInSec;
    }

    /**
     * Frequency (in seconds) at which the revisions bin cleaner executes cleaning. The default value is 5 minutes.
     * @param cleanerFrequencyInSec cleaner frequency (in seconds)
     */
    public void setCleanerFrequencyInSec(long cleanerFrequencyInSec) {
        this.cleanerFrequencyInSec = cleanerFrequencyInSec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RevisionsBinConfiguration that = (RevisionsBinConfiguration) o;
        return disabled == that.disabled
                && minimumEntriesAgeToKeepInMin == that.minimumEntriesAgeToKeepInMin
                && cleanerFrequencyInSec == that.cleanerFrequencyInSec;
    }

    @Override
    public int hashCode() {
        int hashCode = Boolean.hashCode(disabled);
        hashCode = (hashCode * 397) ^ Integer.hashCode(minimumEntriesAgeToKeepInMin);
        hashCode = (hashCode * 397) ^ Long.hashCode(cleanerFrequencyInSec);
        return hashCode;
    }
}
