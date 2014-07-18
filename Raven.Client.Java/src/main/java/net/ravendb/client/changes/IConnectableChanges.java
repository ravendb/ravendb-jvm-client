package net.ravendb.client.changes;

import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.VoidArgs;


public interface IConnectableChanges {
  boolean isConnected();

  public void addConnectionStatusChanged(EventHandler<VoidArgs> handler);

  public void removeConnectionStatusChanges(EventHandler<VoidArgs> handler);
  void waitForAllPendingSubscriptions();
}
