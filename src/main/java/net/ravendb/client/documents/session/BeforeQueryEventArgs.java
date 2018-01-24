package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class BeforeQueryEventArgs extends EventArgs {

    private final InMemoryDocumentSessionOperations session;

    public BeforeQueryEventArgs(InMemoryDocumentSessionOperations session) {
        this.session = session;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return session;
    }

}
