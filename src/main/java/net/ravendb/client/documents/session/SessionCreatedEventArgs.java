package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class SessionCreatedEventArgs extends EventArgs {
    private final InMemoryDocumentSessionOperations session;

    public SessionCreatedEventArgs(InMemoryDocumentSessionOperations session) {
        this.session = session;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return session;
    }
}
