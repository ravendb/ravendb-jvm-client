package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.indexes.*;

import java.util.Date;

public class IndexInformation extends EssentialIndexInformation {

    @JsonProperty("IsStale")
    private boolean stale;
    private IndexState state;
    private Date lastIndexingTime;

    public boolean isStale() {
        return stale;
    }

    public void setStale(boolean stale) {
        this.stale = stale;
    }

    public IndexState getState() {
        return state;
    }

    public void setState(IndexState state) {
        this.state = state;
    }

    public Date getLastIndexingTime() {
        return lastIndexingTime;
    }

    public void setLastIndexingTime(Date lastIndexingTime) {
        this.lastIndexingTime = lastIndexingTime;
    }
}
