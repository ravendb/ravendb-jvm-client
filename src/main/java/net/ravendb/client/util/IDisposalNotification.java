package net.ravendb.client.util;

import net.ravendb.client.primitives.CleanCloseable;

public interface IDisposalNotification extends CleanCloseable {

    boolean isDisposed();
}
