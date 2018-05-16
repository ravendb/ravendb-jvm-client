package net.ravendb.client.documents.changes;

public interface IObserver<T> extends Comparable {
    void onNext(T value);

    void onError(Exception error);

    void onCompleted();
}
