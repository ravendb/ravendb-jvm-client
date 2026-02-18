package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.CleanCloseable;

import java.util.function.Consumer;

public interface IChangesConnectionState<T> extends CleanCloseable {
    int inc();

    int dec();

    void error(Exception e);

    void addOnChangeNotification(ChangesType type, Consumer<T> handler, Consumer<Exception> onError);

    void removeOnChangeNotification(ChangesType type, Consumer<T> handler, Consumer<Exception> onError);
}