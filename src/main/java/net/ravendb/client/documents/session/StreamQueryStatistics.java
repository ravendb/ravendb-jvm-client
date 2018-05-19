package net.ravendb.client.documents.session;

import java.util.Date;

public class StreamQueryStatistics {

    private String indexName;
    private boolean stale;
    private Date indexTimestamp;
    private int totalResults;
    private long resultEtag;


    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public Date getIndexTimestamp() {
        return indexTimestamp;
    }

    public void setIndexTimestamp(Date indexTimestamp) {
        this.indexTimestamp = indexTimestamp;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public long getResultEtag() {
        return resultEtag;
    }

    public void setResultEtag(long resultEtag) {
        this.resultEtag = resultEtag;
    }
}
