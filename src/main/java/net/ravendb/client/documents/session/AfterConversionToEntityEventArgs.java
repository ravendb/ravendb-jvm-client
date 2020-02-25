package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.primitives.EventArgs;

public class AfterConversionToEntityEventArgs extends EventArgs {

    private String _id;
    private ObjectNode _document;
    private Object _entity;
    private InMemoryDocumentSessionOperations _session;

    public AfterConversionToEntityEventArgs(InMemoryDocumentSessionOperations session, String id, ObjectNode document, Object entity) {
        _session = session;
        _id = id;
        _document = document;
        _entity = entity;
    }

    public String getId() {
        return _id;
    }

    public ObjectNode getDocument() {
        return _document;
    }

    public Object getEntity() {
        return _entity;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return _session;
    }
}
