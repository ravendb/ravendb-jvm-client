package net.ravendb.client.documents.operations.backups;

public class LastRaftIndex {
    private Long lastEtag;
    private Long lastFullBackupEtag;

    public Long getLastEtag() {
        return lastEtag;
    }

    public void setLastEtag(Long lastEtag) {
        this.lastEtag = lastEtag;
    }

    public Long getLastFullBackupEtag() {
        return lastFullBackupEtag;
    }

    public void setLastFullBackupEtag(Long lastFullBackupEtag) {
        this.lastFullBackupEtag = lastFullBackupEtag;
    }
}
