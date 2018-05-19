package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.session.IMetadataDictionary;

public class StreamResult<T> {
    private String id;
    private String changeVector;
    private IMetadataDictionary metadata;
    private T document;

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

    public IMetadataDictionary getMetadata() {
        return metadata;
    }

    public void setMetadata(IMetadataDictionary metadata) {
        this.metadata = metadata;
    }

    public T getDocument() {
        return document;
    }

    public void setDocument(T document) {
        this.document = document;
    }
}
