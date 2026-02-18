package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.EventHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AbstractDatabaseConnectionState {

    private List<Consumer<Exception>> onError = new ArrayList<>();

    private final Runnable _onDisconnect;
    public final Runnable onConnect;

    private final AtomicInteger _value = new AtomicInteger(0);
    public Exception lastException;

    private volatile boolean disposed;

    private final CompletableFuture<Void> firstSet = new CompletableFuture<>();
    private CompletableFuture<Void> connected;
    protected final Object eventLock = new Object();

    public void set(CompletableFuture<Void> connection) {
        if (!firstSet.isDone()) {
            connection.whenComplete((res, ex) -> {
                if (ex != null) {
                    firstSet.completeExceptionally(ex);
                } else {
                    firstSet.complete(null);
                }
            });
        }
        connected = connection;
    }

    public void addOnError(Consumer<Exception> handler) {
        this.onError.add(handler);
    }

    public void removeOnError(Consumer<Exception> handler) {
        this.onError.remove(handler);
    }

    protected AbstractDatabaseConnectionState(Runnable onConnect, Runnable onDisconnect) {
        this.onConnect = onConnect;
        this._onDisconnect = onDisconnect;
        _value.set(0);
    }

    public int inc() {
        return _value.incrementAndGet();
    }

    public int dec() {
        int val = _value.decrementAndGet();
        if (val == 0) {
            _onDisconnect.run();

            set(java.util.concurrent.CompletableFuture.completedFuture(null));
        }
        return val;
    }

    public void error(Exception e) {
        lastException = e;
        EventHelper.invoke(onError, e);
    }

    protected <T> void callEventInternal(List<Consumer<T>> handlers, T change) {
        List<Consumer<T>> snapshot;

        synchronized (eventLock) {
            if (handlers.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(handlers);
        }

        for (Consumer<T> handler : snapshot) {
            handler.accept(change);
        }
    }

    protected <T> void registerEventsInternal(
            List<Consumer<T>> listeners,
            Consumer<T> changeHandler,
            Consumer<Exception> errorHandler) {

        synchronized (eventLock) {
            if (disposed)
                return;

            listeners.add(changeHandler);
            onError.add(errorHandler);
        }
    }

    protected <T> void unregisterEventsInternal(
            List<Consumer<T>> onChangeHandlers,
            Consumer<T> changeHandler,
            Consumer<Exception> errorHandler) {

        synchronized (eventLock) {
            if (disposed) {
                return;
            }

            onChangeHandlers.remove(changeHandler);
            onError.remove(errorHandler);
        }
    }

    public void close() {
        synchronized (eventLock) {
            if (disposed) {
                return;
            }

            disposed = true;

            if (connected != null && !connected.isCompletedExceptionally()) {
                CompletableFuture<Void> failed = new CompletableFuture<>();
                failed.completeExceptionally(new IllegalStateException("DatabaseConnectionState is disposed"));
                set(failed);
            }

            onError.clear();
        }
    }
}
