package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.CleanCloseable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class ChangesObservable<T, TConnectionState extends IChangesConnectionState> implements IChangesObservable<T> {

    private final ChangesType _type;
    private final TConnectionState _connectionState;
    private final Function<T, Boolean> _filter;
    private ConcurrentHashMap<IObserver<T>, Boolean> _subscribers = new ConcurrentHashMap<>();
    protected final Object registerLock = new Object();


    ChangesObservable(ChangesType type, TConnectionState connectionState, Function<T, Boolean> filter) {
        _type = type;
        _connectionState = connectionState;
        _filter = filter;
    }

    @SuppressWarnings("unchecked")
    public CleanCloseable subscribe(IObserver<T> observer) {
        final Consumer<T> consumer = this::send;
        final Consumer<Exception> onErrorHandle = this::error;

        boolean isFirst = tryRegisterFirstObserver(observer);

        if (isFirst) {
            _connectionState.inc();
            _connectionState.addOnChangeNotification(_type,consumer, onErrorHandle);
        } else {
            Boolean previous = _subscribers.putIfAbsent(observer, Boolean.TRUE);
            if (previous == null) {
                _connectionState.inc();
            }
        }

        return () -> {
            disposeInternal(observer);
        };
    }

    private void disposeInternal(IObserver<T> observer) {
        final Consumer<T> consumer = this::send;
        final Consumer<Exception> onErrorHandle = this::error;
        boolean removed = _subscribers.remove(observer) != null;

        if (removed) {
            int remaining = _connectionState.dec();

            if (remaining == 0) {
                _connectionState.removeOnChangeNotification(_type, consumer, onErrorHandle);
            }
        }
    }

    private boolean tryRegisterFirstObserver(IObserver<T> observer) {
        if (_subscribers.isEmpty()) {
            synchronized (registerLock){
                if (_subscribers.isEmpty() == false){
                    return false;
                }
                _subscribers.put(observer, true);
                return true;
            }
        }

        return false;
    }

    public void send(T msg) {
        if (_filter != null) {
            try {
                if (!_filter.apply(msg)) {
                    return;
                }
            } catch (Exception e) {
                error(e);
                return;
            }
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
