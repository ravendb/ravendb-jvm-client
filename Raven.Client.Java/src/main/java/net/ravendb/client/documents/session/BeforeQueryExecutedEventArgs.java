package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class BeforeQueryExecutedEventArgs extends EventArgs {

    private final InMemoryDocumentSessionOperations session;

    public BeforeQueryExecutedEventArgs(InMemoryDocumentSessionOperations session) {
        this.session = session;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return session;
    }

}
