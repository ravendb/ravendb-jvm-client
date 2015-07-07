package net.ravendb.client.changes;

import net.ravendb.abstractions.basic.EventHelper;
import net.ravendb.abstractions.basic.ExceptionEventArgs;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Action1;

import java.util.ArrayList;
import java.util.List;

public abstract class ConnectionStateBase implements IChangesConnectionState {

    private List<Action1<ExceptionEventArgs>> onError = new ArrayList<>();
    private final Action0 onZero;
    private int value;

    public ConnectionStateBase(Action0 onZero) {
        value = 0;
        this.onZero = onZero;
    }

    @Override
    public void error(Exception e) {
        EventHelper.invoke(onError, new ExceptionEventArgs(e));
    }

    public List<Action1<ExceptionEventArgs>> getOnError() {
        return onError;
    }

    protected abstract void ensureConnection();

    public void inc() {
        synchronized (this) {
            if (++value == 1)  {
                ensureConnection();
            }
        }
    }

    public void dec() {
        synchronized (this) {
            if (--value == 0) {
                onZero.apply();
            }
        }
    }
}
