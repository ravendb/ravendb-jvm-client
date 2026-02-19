package net.ravendb.client.documents.changes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DatabaseConnectionState<T> extends AbstractDatabaseConnectionState implements IChangesConnectionState<T> {

    private final List<Consumer<DocumentChange>> onDocumentChangeNotification = new ArrayList<>();

    private final List<Consumer<CounterChange>> onCounterChangeNotification = new ArrayList<>();

    private final List<Consumer<IndexChange>> onIndexChangeNotification = new ArrayList<>();

    private final List<Consumer<OperationStatusChange>> onOperationStatusChangeNotification = new ArrayList<>();

    private final List<Consumer<AggressiveCacheChange>> onAggressiveChangeChangeNotification = new ArrayList<>();

    private final List<Consumer<TimeSeriesChange>> onTimeSeriesChangeNotification = new ArrayList<>();


    public DatabaseConnectionState(Runnable onConnect, Runnable onDisconnect) {
        super(onConnect, onDisconnect);
    }

    public void send(DocumentChange documentChange) {
        callEventInternal(onDocumentChangeNotification, documentChange);
    }

    public void send(IndexChange indexChange) {
        callEventInternal(onIndexChangeNotification, indexChange);
    }

    public void send(OperationStatusChange operationStatusChange) {
        callEventInternal(onOperationStatusChangeNotification, operationStatusChange);
    }

    public void send(CounterChange counterChange) {
        callEventInternal(onCounterChangeNotification, counterChange);
    }

    public void send(TimeSeriesChange timeSeriesChange) {
        callEventInternal(onTimeSeriesChangeNotification, timeSeriesChange);
    }

    public void send(AggressiveCacheChange change) {
        callEventInternal(onAggressiveChangeChangeNotification, change);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addOnChangeNotification(ChangesType type, Consumer<T> handler, Consumer<Exception> onError) {
        switch (type) {
            case AGGRESSIVE_CACHE:
                registerEventsInternal(onAggressiveChangeChangeNotification, (Consumer<AggressiveCacheChange>)(Consumer<?>) handler, onError);
                break;
            case DOCUMENT:
                registerEventsInternal(onDocumentChangeNotification, (Consumer<DocumentChange>)(Consumer<?>) handler, onError);
                break;
            case INDEX:
                registerEventsInternal(onIndexChangeNotification, (Consumer<IndexChange>)(Consumer<?>) handler, onError);
                break;
            case OPERATION:
                registerEventsInternal(onOperationStatusChangeNotification, (Consumer<OperationStatusChange>)(Consumer<?>) handler, onError);
                break;
            case COUNTER:
                registerEventsInternal(onCounterChangeNotification, (Consumer<CounterChange>)(Consumer<?>) handler, onError);
                break;
            case TIME_SERIES:
                registerEventsInternal(onTimeSeriesChangeNotification, (Consumer<TimeSeriesChange>)(Consumer<?>) handler, onError);
                break;
            default:
                throw new IllegalStateException("ChangeType: " + type + " is not supported");
        }
    }

    @Override
    public void removeOnChangeNotification(ChangesType type, Consumer handler, Consumer onError) {
        switch (type) {
            case AGGRESSIVE_CACHE:
                unregisterEventsInternal(onAggressiveChangeChangeNotification, (Consumer<AggressiveCacheChange>)(Consumer<?>) handler, onError);
                break;
            case DOCUMENT:
                unregisterEventsInternal(onDocumentChangeNotification, (Consumer<DocumentChange>)(Consumer<?>) handler, onError);
                break;
            case INDEX:
                unregisterEventsInternal(onIndexChangeNotification, (Consumer<IndexChange>)(Consumer<?>) handler, onError);
                break;
            case OPERATION:
                unregisterEventsInternal(onOperationStatusChangeNotification, (Consumer<OperationStatusChange>)(Consumer<?>) handler, onError);
                break;
            case COUNTER:
                unregisterEventsInternal(onCounterChangeNotification, (Consumer<CounterChange>)(Consumer<?>) handler, onError);
                break;
            case TIME_SERIES:
                unregisterEventsInternal(onTimeSeriesChangeNotification, (Consumer<TimeSeriesChange>)(Consumer<?>) handler, onError);
                break;
            default:
                throw new IllegalStateException("ChangeType: " + type + " is not supported");
        }
    }

    @Override
    public void close() {
        synchronized (eventLock){
            super.close();
            onDocumentChangeNotification.clear();
            onIndexChangeNotification.clear();
            onOperationStatusChangeNotification.clear();
            onCounterChangeNotification.clear();
            onTimeSeriesChangeNotification.clear();
            onAggressiveChangeChangeNotification.clear();
        }
    }
}
