package net.ravendb.client.documents.changes;

import net.ravendb.client.primitives.CleanCloseable;

public interface IObservable<T> {

    CleanCloseable subscribe(IObserver<T> observer);
}
