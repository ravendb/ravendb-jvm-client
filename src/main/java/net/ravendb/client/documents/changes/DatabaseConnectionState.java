package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.EventHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DatabaseConnectionState implements IChangesConnectionState {

    private List<Consumer<Exception>> onError = new ArrayList<>();

    public void addOnError(Consumer<Exception> handler) {
        this.onError.add(handler);
    }

    public void removeOnError(Consumer<Exception> handler) {
        this.onError.remove(handler);
    }

    private final Runnable _onDisconnect;
    public final Runnable onConnect;

    private AtomicInteger _value = new AtomicInteger();
    public Exception lastException;

    @Override
    public void inc() {
        _value.incrementAndGet();
    }

    @Override
    public void dec() {
        if (_value.decrementAndGet() == 0) {
            _onDisconnect.run();
        }
    }

    @Override
    public void error(Exception e) {
        lastException = e;
        EventHelper.invoke(onError, e);
    }

    @Override
    public void close() {
    }

    public DatabaseConnectionState(Runnable onConnect, Runnable onDisconnect) {
        this.onConnect = onConnect;
        _onDisconnect = onDisconnect;
        _value.set(0);
    }

    private List<Consumer<DocumentChange>> onDocumentChangeNotification = new ArrayList<>();

    private List<Consumer<IndexChange>> onIndexChangeNotification = new ArrayList<>();

    private List<Consumer<OperationStatusChange>> onOperationStatusChangeNotification = new ArrayList<>();

    public void addOnDocumentChangeNotification(Consumer<DocumentChange> handler) {
        this.onDocumentChangeNotification.add(handler);
    }

    public void removeOnDocumentChangeNotification(Consumer<DocumentChange> handler) {
        this.onDocumentChangeNotification.remove(handler);
    }

    public void addOnIndexChangeNotification(Consumer<IndexChange> handler) {
        this.onIndexChangeNotification.add(handler);
    }

    public void removeOnIndexChangeNotification(Consumer<IndexChange> handler) {
        this.onIndexChangeNotification.remove(handler);
    }

    public void addOnOperationStatusChangeNotification(Consumer<OperationStatusChange> handler) {
        this.onOperationStatusChangeNotification.add(handler);
    }

    public void removeOnOperationStatusChangeNotification(Consumer<OperationStatusChange> handler) {
        this.onOperationStatusChangeNotification.remove(handler);
    }

    public void send(DocumentChange documentChange) {
        EventHelper.invoke(onDocumentChangeNotification, documentChange);
    }

    public void send(IndexChange indexChange) {
        EventHelper.invoke(onIndexChangeNotification, indexChange);
    }

    public void send(OperationStatusChange operationStatusChange) {
        EventHelper.invoke(onOperationStatusChangeNotification, operationStatusChange);
    }
}
