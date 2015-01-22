package net.ravendb.client.changes;


public abstract class ObserverAdapter<T> implements IObserver<T> {
  @Override
  public void onError(Exception error) {
    //empty by design
  }

  @Override
  public void onCompleted() {
    //empty by design
  }
}
