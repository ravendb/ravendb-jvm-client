package net.ravendb.client.documents.operations.backups;

public class LastRaftIndex {
    private Long lastEtag;

    public Long getLastEtag() {
        return lastEtag;
    }

    public void setLastEtag(Long lastEtag) {
        this.lastEtag = lastEtag;
    }
}
