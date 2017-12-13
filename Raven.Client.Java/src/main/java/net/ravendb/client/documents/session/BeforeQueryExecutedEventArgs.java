package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class BeforeQueryExecutedEventArgs extends EventArgs {

    private InMemoryDocumentSessionOperations session;
    private IDocumentQueryCustomization queryCustomization;

    public BeforeQueryExecutedEventArgs(InMemoryDocumentSessionOperations session, IDocumentQueryCustomization queryCustomization) {
        this.session = session;
        this.queryCustomization = queryCustomization;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return session;
    }

    public IDocumentQueryCustomization<IDocumentQueryCustomization> getQueryCustomization() {
        return queryCustomization;
    }
}
