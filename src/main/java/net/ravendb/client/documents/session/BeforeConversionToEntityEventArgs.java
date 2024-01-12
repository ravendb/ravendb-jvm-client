package net.ravendb.client.documents.session;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.primitives.EventArgs;
import net.ravendb.client.primitives.Reference;

public class BeforeConversionToEntityEventArgs extends EventArgs {

    private final String _id;
    private final Class _type;
    private final Reference<ObjectNode> _document;
    private final InMemoryDocumentSessionOperations _session;

    public BeforeConversionToEntityEventArgs(InMemoryDocumentSessionOperations session, String id, Class type, Reference<ObjectNode> document) {
        _session = session;
        _id = id;
        _type = type;
        _document = document;
    }

    public String getId() {
        return _id;
    }

    public Class getType() {
        return _type;
    }

    public Reference<ObjectNode> getDocument() {
        return _document;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return _session;
    }
}
