package net.ravendb.client.documents.changes;

public interface IObserver<T> {
    void onNext(T value);

    @SuppressWarnings("EmptyMethod")
    void onError(Exception error);

    @SuppressWarnings("EmptyMethod")
    void onCompleted();
}
