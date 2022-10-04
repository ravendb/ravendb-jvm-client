package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Date;

public class PatchResultBase {

    private PatchStatus status;
    private ObjectNode modifiedDocument;

    private Date lastModified;

    private String changeVector;
    private String collection;

    public PatchStatus getStatus() {
        return status;
    }

    public void setStatus(PatchStatus status) {
        this.status = status;
    }

    public ObjectNode getModifiedDocument() {
        return modifiedDocument;
    }

    public void setModifiedDocument(ObjectNode modifiedDocument) {
        this.modifiedDocument = modifiedDocument;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
}
