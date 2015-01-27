package net.ravendb.client.connection;

import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;

public class Operation {
  private long id;
  private RavenJToken state;
  private Function1<Long, RavenJToken> statusFetcher;

  public Operation(final ServerClient client, long id) {
    this(new Function1<Long, RavenJToken>() {
      @SuppressWarnings("boxing")
      @Override
      public RavenJToken apply(Long input) {
        return client.getOperationStatus(input);
      }
    }, id);
    this.id = id;
  }

  public Operation(Function1<Long, RavenJToken> statusFetcher, long id) {
    this.statusFetcher = statusFetcher;
    this.id = id;
  }

  public Operation(long id, RavenJToken state) {
    super();
    this.id = id;
    this.state = state;
  }

  @SuppressWarnings("boxing")
  public RavenJToken waitForCompletion() {
    if (statusFetcher == null)
      return state;

    while (true) {
      RavenJToken status = statusFetcher.apply(id);
      if (status == null) {
        return null;
      }
      if (status.value(Boolean.class, "Completed")) {
        boolean faulted = status.value(Boolean.TYPE, "Faulted");
        if (faulted) {
          RavenJObject error = status.value(RavenJObject.class, "State");
          String errorMessage = error.value(String.class, "Error");
          throw new IllegalStateException("Operation failed: " + errorMessage);
        }

        return status.value(RavenJToken.class, "State");
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        /*ignore */
      }
    }
  }

}
