package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.primitives.EventArgs;

public class AfterConversionToEntityEventArgs extends EventArgs {

    private final String _id;
    private final ObjectNode _document;
    private final Object _entity;
    private final InMemoryDocumentSessionOperations _session;

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
