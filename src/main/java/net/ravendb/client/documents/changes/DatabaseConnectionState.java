package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.EventHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DatabaseConnectionState implements IChangesConnectionState<DatabaseChange> {

    private final List<Consumer<Exception>> onError = new ArrayList<>();

    public void addOnError(Consumer<Exception> handler) {
        this.onError.add(handler);
    }

    public void removeOnError(Consumer<Exception> handler) {
        this.onError.remove(handler);
    }

    private final Runnable _onDisconnect;
    public final Runnable onConnect;

    private final AtomicInteger _value = new AtomicInteger();
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
        onDocumentChangeNotification.clear();
        onIndexChangeNotification.clear();
        onOperationStatusChangeNotification.clear();
        onCounterChangeNotification.clear();
        onTimeSeriesChangeNotification.clear();
        onError.clear();
        onAggressiveChangeChangeNotification.clear();
    }

    public DatabaseConnectionState(Runnable onConnect, Runnable onDisconnect) {
        this.onConnect = onConnect;
        _onDisconnect = onDisconnect;
        _value.set(0);
    }


    @SuppressWarnings("unchecked")
    public void addOnChangeNotification(ChangesType type, Consumer<DatabaseChange> handler) {
        switch (type) {
            case AGGRESSIVE_CACHE:
                this.onAggressiveChangeChangeNotification.add((Consumer<AggressiveCacheChange>)(Consumer<?>) handler);
                break;
            case DOCUMENT:
                this.onDocumentChangeNotification.add((Consumer<DocumentChange>)(Consumer<?>) handler);
                break;
            case INDEX:
                this.onIndexChangeNotification.add((Consumer<IndexChange>)(Consumer<?>) handler);
                break;
            case OPERATION:
                this.onOperationStatusChangeNotification.add((Consumer<OperationStatusChange>)(Consumer<?>) handler);
                break;
            case COUNTER:
                this.onCounterChangeNotification.add((Consumer<CounterChange>)(Consumer<?>) handler);
                break;
            case TIME_SERIES:
                this.onTimeSeriesChangeNotification.add((Consumer<TimeSeriesChange>)(Consumer<?>) handler);
                break;
            default:
                throw new IllegalStateException("ChangeType: " + type + " is not supported");
        }
    }

    public void removeOnChangeNotification(ChangesType type, Consumer<DatabaseChange> handler) {
        switch (type) {
            case AGGRESSIVE_CACHE:
                this.onAggressiveChangeChangeNotification.remove(handler);
                break;
            case DOCUMENT:
                this.onDocumentChangeNotification.remove(handler);
                break;
            case INDEX:
                this.onIndexChangeNotification.remove(handler);
                break;
            case OPERATION:
                this.onOperationStatusChangeNotification.remove(handler);
                break;
            case COUNTER:
                this.onCounterChangeNotification.remove(handler);
                break;
            case TIME_SERIES:
                this.onTimeSeriesChangeNotification.remove(handler);
                break;
            default:
                throw new IllegalStateException("ChangeType: " + type + " is not supported");
        }
    }

    private final List<Consumer<DocumentChange>> onDocumentChangeNotification = new ArrayList<>();

    private final List<Consumer<IndexChange>> onIndexChangeNotification = new ArrayList<>();

    private final List<Consumer<OperationStatusChange>> onOperationStatusChangeNotification = new ArrayList<>();

    private final List<Consumer<CounterChange>> onCounterChangeNotification = new ArrayList<>();

    private final List<Consumer<TimeSeriesChange>> onTimeSeriesChangeNotification = new ArrayList<>();
    private final List<Consumer<AggressiveCacheChange>> onAggressiveChangeChangeNotification = new ArrayList<>();

    public void send(DocumentChange documentChange) {
        EventHelper.invoke(onDocumentChangeNotification, documentChange);
    }

    public void send(IndexChange indexChange) {
        EventHelper.invoke(onIndexChangeNotification, indexChange);
    }

    public void send(OperationStatusChange operationStatusChange) {
        EventHelper.invoke(onOperationStatusChangeNotification, operationStatusChange);
    }

    public void send(CounterChange counterChange) {
        EventHelper.invoke(onCounterChangeNotification, counterChange);
    }

    public void send(TimeSeriesChange timeSeriesChange) {
        EventHelper.invoke(onTimeSeriesChangeNotification, timeSeriesChange);
    }

    public void send(AggressiveCacheChange change) {
        EventHelper.invoke(onAggressiveChangeChangeNotification, change);
    }
}
