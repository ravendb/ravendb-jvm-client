package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class BeforeConversionToDocumentEventArgs extends EventArgs {

    private String _id;
    private Object _entity;
    private InMemoryDocumentSessionOperations _session;

    public BeforeConversionToDocumentEventArgs(InMemoryDocumentSessionOperations session, String id, Object entity) {
        _session = session;
        _id = id;
        _entity = entity;
    }

    public String getId() {
        return _id;
    }

    public Object getEntity() {
        return _entity;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return _session;
    }
}
