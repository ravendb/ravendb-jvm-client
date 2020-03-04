package net.ravendb.client;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;

public class DatabaseCommands implements CleanCloseable {

    private final IDocumentStore _store;
    private final RequestExecutor _requestExecutor;
    private final InMemoryDocumentSessionOperations _session;

    public DatabaseCommands(IDocumentStore store, String databaseName) {
        if (store == null) {
            throw new IllegalArgumentException("Store cannot be null");
        }
        _store = store;

        _session = (InMemoryDocumentSessionOperations) _store.openSession(databaseName);
        _requestExecutor = store.getRequestExecutor(databaseName);
    }

    public IDocumentStore getStore() {
        return _store;
    }

    public RequestExecutor getRequestExecutor() {
        return _requestExecutor;
    }

    public InMemoryDocumentSessionOperations getSession() {
        return _session;
    }

    public static DatabaseCommands forStore(IDocumentStore store) {
        return forStore(store, null);
    }

    public static DatabaseCommands forStore(IDocumentStore store, String databaseName) {
        return new DatabaseCommands(store, databaseName);
    }

    public <TResult> void execute(RavenCommand<TResult> command) {
        _requestExecutor.execute(command);
    }

    @Override
    public void close() {
        _session.close();
    }
}
