package net.ravendb.client.documents.indexes;

import java.util.Date;

public class RollingIndexDeployment {
    private RollingIndexState state;
    private Date createdAt;
    private Date startedAt;
    private Date finishedAt;

    public RollingIndexState getState() {
        return state;
    }

    public void setState(RollingIndexState state) {
        this.state = state;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Date finishedAt) {
        this.finishedAt = finishedAt;
    }
}
