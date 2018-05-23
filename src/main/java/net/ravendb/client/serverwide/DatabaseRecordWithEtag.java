package net.ravendb.client.serverwide;

public class DatabaseRecordWithEtag extends DatabaseRecord {
    private long etag;

    public long getEtag() {
        return etag;
    }

    public void setEtag(long etag) {
        this.etag = etag;
    }
}
