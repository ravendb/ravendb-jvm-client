package net.ravendb.client.utils;

import net.ravendb.abstractions.exceptions.OperationCancelledException;


public class CancellationTokenSource {

  boolean cancelled = false;

  public CancellationToken getToken() {
    return new CancellationToken();
  }

  public class CancellationToken {
    protected CancellationToken() {

    }

    public void throwIfCancellationRequested() {
      if (cancelled) {
        throw new OperationCancelledException();
      }
    }
  }

  public void cancel() {
    cancelled = true;
  }
}
