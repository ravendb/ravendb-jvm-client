package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class BeforeQueryEventArgs extends EventArgs {

    private final InMemoryDocumentSessionOperations session;
    private final IDocumentQueryCustomization queryCustomization;

    public BeforeQueryEventArgs(InMemoryDocumentSessionOperations session, IDocumentQueryCustomization queryCustomization) {
        this.session = session;
        this.queryCustomization = queryCustomization;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return session;
    }

    public IDocumentQueryCustomization getQueryCustomization() {
        return queryCustomization;
    }
}
