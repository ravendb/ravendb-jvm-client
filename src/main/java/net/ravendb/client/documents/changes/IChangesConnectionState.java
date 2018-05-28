package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.CleanCloseable;

import java.util.function.Consumer;

public interface IChangesConnectionState<T> extends CleanCloseable {
    void inc();

    void dec();

    void error(Exception e);

    void addOnChangeNotification(ChangesType type, Consumer<T> handler);

    void removeOnChangeNotification(ChangesType type, Consumer<T> handler);

    void addOnError(Consumer<Exception> handler);

    void removeOnError(Consumer<Exception> handler);
}