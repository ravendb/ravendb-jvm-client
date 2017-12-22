package net.ravendb.client.util;

import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.EventHandler;
import net.ravendb.client.primitives.VoidArgs;

public interface IDisposalNotification extends CleanCloseable {
    void addAfterCloseListener(EventHandler<VoidArgs> event);

    void removeAfterCloseListener(EventHandler<VoidArgs> event);

    boolean isDisposed();
}
