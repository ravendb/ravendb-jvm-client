package net.ravendb.client.documents.operations.backups;

public class LastRaftIndex {
    private long lastEtag;

    public long getLastEtag() {
        return lastEtag;
    }

    public void setLastEtag(long lastEtag) {
        this.lastEtag = lastEtag;
    }
}
