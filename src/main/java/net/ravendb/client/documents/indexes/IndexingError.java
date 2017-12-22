package net.ravendb.client.documents.indexes;

import java.util.Date;

public class IndexingError {

    private String error;
    private Date timestamp;
    private String document;
    private String action;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "Error: " + error + ", Document: " + document + ", Action: " + action;
    }

}
