package net.ravendb.client.shard;

import java.util.ArrayList;
import java.util.List;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Predicate;
import net.ravendb.client.changes.IObservable;
import net.ravendb.client.changes.IObserver;


public class ShardedObservable<T> implements IObservable<T> {
  private final List<IObservable<T>> inner;

  public ShardedObservable(List<IObservable<T>> inner) {
    this.inner = inner;
  }

  @Override
  public CleanCloseable subscribe(IObserver<T> observer) {
    final List<CleanCloseable> closeables = new ArrayList<>();
    for (IObservable<T> observable: inner) {
      closeables.add(observable.subscribe(observer));
    }
    return new CleanCloseable() {
      @Override
      public void close() {
        for (CleanCloseable closeable: closeables) {
          closeable.close();
        }
      }
    };
  }

  @Override
  public IObservable<T> where(Predicate<T> predicate) {
    throw new UnsupportedOperationException("Where is not supported");
  }
}
