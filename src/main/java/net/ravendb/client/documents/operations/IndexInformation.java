package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.ravendb.client.documents.indexes.*;

import java.util.Date;

public class IndexInformation {
    private String name;

    @JsonProperty("IsStale")
    private boolean stale;
    private IndexState state;
    private IndexLockMode lockMode;
    private IndexPriority priority;
    private IndexType type;
    private Date lastIndexingTime;
    private IndexSourceType sourceType;

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

    public IndexState getState() {
        return state;
    }

    public void setState(IndexState state) {
        this.state = state;
    }

    public IndexLockMode getLockMode() {
        return lockMode;
    }

    public void setLockMode(IndexLockMode lockMode) {
        this.lockMode = lockMode;
    }

    public IndexPriority getPriority() {
        return priority;
    }

    public void setPriority(IndexPriority priority) {
        this.priority = priority;
    }

    public IndexType getType() {
        return type;
    }

    public void setType(IndexType type) {
        this.type = type;
    }

    public Date getLastIndexingTime() {
        return lastIndexingTime;
    }

    public void setLastIndexingTime(Date lastIndexingTime) {
        this.lastIndexingTime = lastIndexingTime;
    }

    public IndexSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(IndexSourceType sourceType) {
        this.sourceType = sourceType;
    }
}
