package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class SessionClosingEventArgs extends EventArgs {
    private InMemoryDocumentSessionOperations _session;

    public SessionClosingEventArgs(InMemoryDocumentSessionOperations session) {
        _session = session;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return _session;
    }
}
