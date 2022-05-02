package net.ravendb.client;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.indexes.IndexErrors;

import java.time.Duration;

public class IndexesTestBase {

    private final RemoteTestBase parent;

    public IndexesTestBase(RemoteTestBase parent) {
        this.parent = parent;
    }

    public void waitForIndexing(IDocumentStore store) {
        waitForIndexing(store, null, null);
    }

    public void waitForIndexing(IDocumentStore store, String database) {
        waitForIndexing(store, database, null);
    }

    public void waitForIndexing(IDocumentStore store, String database, Duration timeout) {
        waitForIndexing(store, database, timeout, null);
    }

    public void waitForIndexing(IDocumentStore store, String database, Duration timeout, String nodeTag) {
        RemoteTestBase.waitForIndexing(store, database, timeout, nodeTag);
    }

    public IndexErrors[] waitForIndexingErrors(IDocumentStore store, Duration timeout, String... indexNames) throws InterruptedException {
        return RemoteTestBase.waitForIndexingErrors(store, timeout, indexNames);
    }
}
