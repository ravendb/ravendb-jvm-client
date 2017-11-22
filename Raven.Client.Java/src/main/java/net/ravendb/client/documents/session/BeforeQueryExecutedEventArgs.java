package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class BeforeQueryExecutedEventArgs extends EventArgs {

    private InMemoryDocumentSessionOperations session;

    public BeforeQueryExecutedEventArgs(InMemoryDocumentSessionOperations session) { //TODO:IDocumentQueryCustomization
        this.session = session;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return session;
    }

    //TODO public IDocumentQueryCustomization QueryCustomization { get; }
}
