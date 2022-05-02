package net.ravendb.client.documents.operations.revisions;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Date;

public class RevisionIncludeResult {
    private String id;
    private String changeVector;
    private Date before;
    private ObjectNode revision;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    public Date getBefore() {
        return before;
    }

    public void setBefore(Date before) {
        this.before = before;
    }

    public ObjectNode getRevision() {
        return revision;
    }

    public void setRevision(ObjectNode revision) {
        this.revision = revision;
    }
}
