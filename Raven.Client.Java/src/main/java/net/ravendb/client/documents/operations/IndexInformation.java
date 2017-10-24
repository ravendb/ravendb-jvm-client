package net.ravendb.client.documents.operations;

public class IndexInformation {

    private long etag;
    private String name;
    private boolean stale;

    public long getEtag() {
        return etag;
    }

    public void setEtag(long etag) {
        this.etag = etag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    /* TODO + test

        public IndexState State { get; set; }

        public IndexLockMode LockMode { get; set; }

        public IndexPriority Priority { get; set; }

        public IndexType Type { get; set; }

        public DateTime? LastIndexingTime { get; set; }
    }
     */
}
