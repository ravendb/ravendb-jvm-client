package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.CleanCloseable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChangesObservable<T, TConnectionState extends IChangesConnectionState> implements IChangesObservable<T> {

    private final ChangesType _type;
    private final TConnectionState _connectionState;
    private final Function<T, Boolean> _filter;
    private final ConcurrentHashMap<IObserver<T>, Boolean> _subscribers = new ConcurrentHashMap<>();


    ChangesObservable(ChangesType type, TConnectionState connectionState, Function<T, Boolean> filter) {
        _type = type;
        _connectionState = connectionState;
        _filter = filter;
    }

    @SuppressWarnings("unchecked")
    public CleanCloseable subscribe(IObserver<T> observer) {
        final Consumer<T> consumer = payload -> this.send(payload);
        final Consumer<Exception> onErrorHandle = ex -> this.error(ex);

        _connectionState.addOnChangeNotification(_type, consumer);
        _connectionState.addOnError(onErrorHandle);

        _connectionState.inc();
        _subscribers.put(observer, true);

        return () -> {
            _connectionState.dec();
            _subscribers.remove(observer);
            _connectionState.removeOnChangeNotification(_type, consumer);
            _connectionState.removeOnError(onErrorHandle);
        };
    }

    public void send(T msg) {
        try {
            if (!_filter.apply(msg)) {
                return;
            }
        } catch (Exception e) {
            error(e);
            return;
        }

        for (IObserver<T> subscriber : _subscribers.keySet()) {
            subscriber.onNext(msg);
        }
    }

    public void error(Exception e) {
        for (IObserver<T> subscriber : _subscribers.keySet()) {
            subscriber.onError(e);
        }
    }
}
