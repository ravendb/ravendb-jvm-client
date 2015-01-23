package net.ravendb.client.changes;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Predicate;



public interface IObservable<T> {
  public CleanCloseable subscribe(IObserver<T> observer);


  public IObservable<T> where(Predicate<T> predicate);

}
