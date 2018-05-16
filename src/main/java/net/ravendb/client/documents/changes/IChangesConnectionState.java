package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.CleanCloseable;

import java.util.concurrent.CompletableFuture;

public interface IChangesConnectionState extends CleanCloseable {
    void inc();

    void dec();

    void error(Exception e);
}
