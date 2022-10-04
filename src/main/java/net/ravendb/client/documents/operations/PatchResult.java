package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Date;

public class PatchResult extends PatchResultBase {

    private ObjectNode originalDocument;
    private ObjectNode debug;

    public ObjectNode getOriginalDocument() {
        return originalDocument;
    }

    public void setOriginalDocument(ObjectNode originalDocument) {
        this.originalDocument = originalDocument;
    }

    public ObjectNode getDebug() {
        return debug;
    }

    public void setDebug(ObjectNode debug) {
        this.debug = debug;
    }

}
