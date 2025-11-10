package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.EventHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AbstractDatabaseConnectionState {

    private final List<Consumer<Exception>> onError = new ArrayList<>();

    private final Runnable _onDisconnect;
    public final Runnable onConnect;

    private final AtomicInteger _value = new AtomicInteger(0);
    public Exception lastException;

    private final CompletableFuture<Void> firstSet = new CompletableFuture<>();
    private CompletableFuture<Void> connected;

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

    public void inc() {
        _value.incrementAndGet();
    }

    public void dec() {
        if (_value.decrementAndGet() == 0) {
            _onDisconnect.run();
        }
    }

    public void error(Exception e) {
        lastException = e;
        EventHelper.invoke(onError, e);
    }

    public void close() {
        onError.clear();
    }
}
