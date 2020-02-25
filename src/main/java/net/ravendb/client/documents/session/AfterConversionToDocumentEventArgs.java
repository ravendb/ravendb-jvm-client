package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.primitives.EventArgs;
import net.ravendb.client.primitives.Reference;

public class AfterConversionToDocumentEventArgs extends EventArgs {

    private String _id;
    private Object _entity;
    private Reference<ObjectNode> _document;
    private InMemoryDocumentSessionOperations _session;

    public AfterConversionToDocumentEventArgs(InMemoryDocumentSessionOperations session, String id, Object entity, Reference<ObjectNode> document) {
        _session = session;
        _id = id;
        _entity = entity;
        _document = document;
    }

    public String getId() {
        return _id;
    }

    public Object getEntity() {
        return _entity;
    }

    public Reference<ObjectNode> getDocument() {
        return _document;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return _session;
    }
}
